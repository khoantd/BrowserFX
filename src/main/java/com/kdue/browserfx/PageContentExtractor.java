package com.kdue.browserfx;

import javafx.scene.web.WebEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PageContentExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageContentExtractor.class);
    private static final int MAX_CONTENT_LENGTH = 8000;

    private final WebEngine webEngine;

    public PageContentExtractor(WebEngine webEngine) {
        this.webEngine = webEngine;
    }

    public String extractContent() {
        if (webEngine.getLocation() == null || webEngine.getLocation().isBlank()) {
            return "";
        }

        String script = buildExtractionScript();
        try {
            Object result = webEngine.executeScript(script);
            if (result != null) {
                String content = result.toString().trim();
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH) + "...";
                }
                LOGGER.debug("Extracted {} characters from page", content.length());
                return content;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to extract page content", e);
        }
        return "";
    }

    public String extractContentWithLinks() {
        if (webEngine.getLocation() == null || webEngine.getLocation().isBlank()) {
            return "";
        }

        String script = buildExtractionWithLinksScript();
        try {
            Object result = webEngine.executeScript(script);
            if (result != null) {
                String full = result.toString().trim();
                String linksHeader = "\n\nAdditional links found on page:\n";
                int idx = full.indexOf(linksHeader);
                if (idx != -1) {
                    String contentPart = full.substring(0, idx);
                    String linksPart = full.substring(idx);
                    if (contentPart.length() > MAX_CONTENT_LENGTH * 2) {
                        contentPart = contentPart.substring(0, MAX_CONTENT_LENGTH * 2) + "...";
                    }
                    return contentPart + linksPart;
                }
                if (full.length() > MAX_CONTENT_LENGTH * 2) {
                    full = full.substring(0, MAX_CONTENT_LENGTH * 2) + "...";
                }
                return full;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to extract page content with links", e);
        }
        return "";
    }

    private String buildExtractionScript() {
        return """
            (function() {
                function extractText(element) {
                    if (!element) return '';
                    
                    var clone = element.cloneNode(true);
                    
                    var unwanted = clone.querySelectorAll('script, style, nav, header, footer, aside, .nav, .menu, .sidebar, .advertisement, .ad, .social, .comment, .hidden, [role="navigation"], [role="banner"], [role="complementary"]');
                    unwanted.forEach(function(el) { el.remove(); });
                    
                    return clone.textContent || clone.innerText || '';
                }
                
                var content = '';
                
                var main = document.querySelector('main');
                if (main) {
                    content = extractText(main);
                }
                
                if (!content || content.length < 100) {
                    var article = document.querySelector('article');
                    if (article) {
                        content = extractText(article);
                    }
                }
                
                if (!content || content.length < 100) {
                    var contentSelectors = ['.content', '.post-content', '.article-content', '.entry-content', '.body-content', '.product-list', '.products', '.shop-grid', '.product-item', '.product-card', '#content', '.main-content', '.items-grid'];
                    for (var i = 0; i < contentSelectors.length; i++) {
                        var el = document.querySelector(contentSelectors[i]);
                        if (el) {
                            content = extractText(el);
                            if (content.length >= 100) break;
                        }
                    }
                }
                
                if (!content || content.length < 100) {
                    content = extractText(document.body);
                }
                
                return content.replace(/\\s+/g, ' ').trim();
            })()
            """;
    }

    private String buildExtractionWithLinksScript() {
        return """
            (function() {
                function extractText(element) {
                    if (!element) return '';
                    var clone = element.cloneNode(true);
                    var unwanted = clone.querySelectorAll('script, style, nav, header, footer, aside, .nav, .menu, .sidebar, .advertisement, .ad, .social, .comment, .hidden');
                    unwanted.forEach(function(el) { el.remove(); });
                    return clone.textContent || clone.innerText || '';
                }
                
                var content = '';
                
                var main = document.querySelector('main');
                if (main) content = extractText(main);
                
                if (!content || content.length < 100) {
                    var article = document.querySelector('article');
                    if (article) content = extractText(article);
                }
                
                if (!content || content.length < 100) {
                    var contentSelectors = ['.content', '.post-content', '.article-content', '.entry-content', '.body-content', '.product-list', '.products', '.shop-grid', '.product-item', '.product-card', '#content', '.main-content'];
                    for (var i = 0; i < contentSelectors.length; i++) {
                        var el = document.querySelector(contentSelectors[i]);
                        if (el) {
                            content = extractText(el);
                            if (content.length >= 100) break;
                        }
                    }
                }
                
                if (!content || content.length < 100) {
                    content = extractText(document.body);
                }
                
                // Extract all links with their text; resolve relative URLs to absolute so the AI gets full links.
                // Prioritize product-detail links (/products/...) so they are not pushed out by nav/footer (30 DOM-order cap).
                // Haravan/Shopify-style: also harvest product links from product cards (title -> /products/ url) when the card has a product link.
                var base = document.baseURI || location.href;
                var allLinks = document.querySelectorAll('a[href]');
                var productLinks = [];
                var otherLinks = [];
                var linkSet = new Set();
                var maxTotal = 50;
                
                function resolveHref(raw) {
                    if (!raw) return null;
                    try { return new URL(raw, base).href; } catch (e) { return null; }
                }
                
                function pathIsProduct(href) {
                    var path = (href.match(/^https?:\\/\\/[^\\/]+(\\/[^?#]*)/) || [])[1] || '';
                    return /\\/products?\\//i.test(path);
                }
                
                // 1) From product-card regions: get product URL + card title (h2/h3/.product-title) so we have product name -> URL even when the <a> has no text (e.g. image link).
                var cardSelectors = ['.product-item', '.product-card', '[class*="product-card"]', '[class*="product-item"]', '.product-block', '.product-col'];
                var seenProductUrl = new Set();
                cardSelectors.forEach(function(sel) {
                    try {
                        document.querySelectorAll(sel).forEach(function(card) {
                            var productAnchor = card.querySelector('a[href*="/products/"], a[href*="/product/"]');
                            if (!productAnchor) return;
                            var rawHref = productAnchor.getAttribute('href');
                            var href = resolveHref(rawHref);
                            if (!href || seenProductUrl.has(href)) return;
                            var titleEl = card.querySelector('h2, h3, .product-title, .product-name, [class*="product-title"], [class*="product-name"]');
                            var text = (titleEl ? titleEl.textContent.trim() : productAnchor.textContent.trim()) || '';
                            if (text.length > 0 && text.length < 200) {
                                seenProductUrl.add(href);
                                productLinks.push(text + ': ' + href);
                            }
                        });
                    } catch (e) {}
                });
                
                // 2) All page links: add product links we didn't get from cards, then other links (dedupe by href).
                allLinks.forEach(function(link) {
                    var rawHref = link.getAttribute('href');
                    if (!rawHref || rawHref.startsWith('javascript:') || rawHref.startsWith('mailto:') || rawHref.startsWith('#')) return;
                    var href = resolveHref(rawHref) || link.href || rawHref;
                    if (!href || linkSet.has(href)) return;
                    var text = link.textContent.trim();
                    if (text.length === 0 || text.length >= 200) return;
                    linkSet.add(href);
                    if (pathIsProduct(href) && !seenProductUrl.has(href)) {
                        seenProductUrl.add(href);
                        productLinks.push(text + ': ' + href);
                    } else if (!pathIsProduct(href)) {
                        otherLinks.push(text + ': ' + href);
                    }
                });
                
                var links = productLinks.concat(otherLinks).slice(0, maxTotal);
                var result = content.replace(/\\s+/g, ' ').trim();
                if (links.length > 0) {
                    result += '\\n\\nAdditional links found on page:\\n' + links.join('\\n');
                }
                
                return result;
            })()
            """;
    }

    public String getPageTitle() {
        try {
            Object result = webEngine.executeScript("document.title");
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            LOGGER.error("Failed to get page title", e);
            return "";
        }
    }

    public String getPageUrl() {
        return webEngine.getLocation();
    }

    /**
     * Extracts product cards from the page (title, url, image URL, price) for display in the AI Companion.
     * On product detail pages (Haravan/Shopify-style URL with /products/), tries to extract a single
     * product with add-to-cart button label when present. Returns an empty list if extraction fails.
     */
    public List<ProductInfo> extractProducts() {
        if (webEngine.getLocation() == null || webEngine.getLocation().isBlank()) {
            return List.of();
        }
        try {
            List<ProductInfo> detail = extractProductDetailIfApplicable();
            if (!detail.isEmpty()) {
                return detail;
            }
        } catch (Exception e) {
            LOGGER.debug("Product detail extraction skipped or failed", e);
        }
        String script = buildExtractProductsScript();
        try {
            Object result = webEngine.executeScript(script);
            if (result == null || result.toString().isBlank()) {
                return List.of();
            }
            return parseProductLines(result.toString());
        } catch (Exception e) {
            LOGGER.debug("Failed to extract products from page", e);
            return List.of();
        }
    }

    /**
     * When the current page URL looks like a product detail page (e.g. /products/...),
     * extracts a single product with title, price, image, and add-to-cart button label
     * (Haravan/Shopify pattern: form[action*="cart"] button or button[name="add"]).
     * Returns empty list if not a product detail page or extraction fails.
     */
    private List<ProductInfo> extractProductDetailIfApplicable() {
        String url = webEngine.getLocation();
        if (url == null || !url.matches("(?i).*/products?/.*")) {
            return List.of();
        }
        String script = buildExtractProductDetailScript();
        try {
            Object result = webEngine.executeScript(script);
            if (result == null || result.toString().isBlank()) {
                return List.of();
            }
            return parseProductLines(result.toString());
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildExtractProductDetailScript() {
        return """
            (function() {
                var base = document.baseURI || location.href;
                function resolve(u) {
                    if (!u) return '';
                    try { return new URL(u, base).href; } catch (e) { return u; }
                }
                function clean(s) {
                    if (!s) return '';
                    return s.replace(/\\t/g, ' ').replace(/\\n/g, ' ').trim().substring(0, 300);
                }
                var title = '';
                var price = '';
                var imageUrl = '';
                var addToCartLabel = '';
                var container = document.querySelector('.product-detail, .product-single, [class*="product-detail"], [class*="product-single"], .product, main, article') || document.body;
                var titleEl = container.querySelector('h1, .product-title, .product-name, [class*="product-title"]');
                title = clean(titleEl ? titleEl.textContent : document.querySelector('h1') ? document.querySelector('h1').textContent : '');
                if (title.length === 0) title = 'Product';
                var priceEl = container.querySelector('.price, .product-price, .amount, [class*="price"], .current_price, .sale-price');
                price = clean(priceEl ? priceEl.textContent : '');
                var img = container.querySelector('img[src]');
                imageUrl = img ? resolve(img.getAttribute('src') || img.getAttribute('data-src')) : '';
                var addBtn = container.querySelector('form[action*="cart"] button[type="submit"], form[action*="cart"] button[name="add"], button[name="add"], .add-to-cart, .btn-add-to-cart, [class*="add-to-cart"]');
                if (!addBtn) {
                    var buttons = container.querySelectorAll('button, [role="button"], input[type="submit"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var t = (buttons[i].textContent || buttons[i].value || '').trim();
                        if (/Thêm vào giỏ|Add to cart|Mua ngay|Add to bag/i.test(t)) {
                            addBtn = buttons[i];
                            break;
                        }
                    }
                }
                if (addBtn) addToCartLabel = clean(addBtn.textContent || addBtn.value || '');
                return title + '\\t' + base + '\\t' + imageUrl + '\\t' + price + '\\t' + addToCartLabel;
            })()
            """;
    }

    private String buildExtractProductsScript() {
        return """
            (function() {
                var base = document.baseURI || location.href;
                function resolve(u) {
                    if (!u) return '';
                    try { return new URL(u, base).href; } catch (e) { return u; }
                }
                function clean(s) {
                    if (!s) return '';
                    return s.replace(/\\t/g, ' ').replace(/\\n/g, ' ').trim().substring(0, 300);
                }
                function getImgUrl(img) {
                    if (!img) return '';
                    var u = img.getAttribute('src') || img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original');
                    if (u) return resolve(u);
                    u = img.getAttribute('data-srcset') || img.getAttribute('srcset');
                    if (u) {
                        var firstEntry = u.split(',')[0];
                        if (firstEntry) { var urlPart = firstEntry.trim().split(/\\s+/)[0]; if (urlPart) return resolve(urlPart); }
                    }
                    return '';
                }
                var cardSelectors = ['.product-item', '.product-card', '[class*="product-card"]', '[class*="product-item"]', '.product-block', '.product-col', '.product', '.grid-product', '[class*="product"]'];
                var seen = new Set();
                var out = [];
                var max = 20;
                cardSelectors.forEach(function(sel) {
                    if (out.length >= max) return;
                    try {
                        document.querySelectorAll(sel).forEach(function(card) {
                            if (out.length >= max) return;
                            var a = card.querySelector('a[href*="/products/"], a[href*="/product/"], a[href]');
                            if (!a) return;
                            var href = resolve(a.getAttribute('href'));
                            if (!href || href.startsWith('javascript:') || seen.has(href)) return;
                            var titleEl = card.querySelector('h2, h3, h4, .product-title, .product-name, [class*="product-title"], [class*="product-name"]');
                            var title = clean((titleEl ? titleEl.textContent : a.textContent) || '');
                            if (title.length === 0) title = 'Product';
                            var img = card.querySelector('img');
                            var imageUrl = getImgUrl(img);
                            var priceEl = card.querySelector('.price, .product-price, .amount, [class*="price"], .current_price, .sale-price');
                            var price = clean(priceEl ? priceEl.textContent : '');
                            seen.add(href);
                            out.push(title + '\\t' + href + '\\t' + imageUrl + '\\t' + price);
                        });
                    } catch (e) {}
                });
                if (out.length === 0) {
                    try {
                        document.querySelectorAll('a[href*="/products/"], a[href*="/product/"]').forEach(function(a) {
                            if (out.length >= max) return;
                            var href = resolve(a.getAttribute('href'));
                            if (!href || href.startsWith('javascript:') || seen.has(href)) return;
                            var parent = a.closest('div, li, article, section') || a.parentElement;
                            if (!parent) return;
                            var title = clean(a.textContent || '');
                            if (title.length === 0) title = (parent.querySelector('h2, h3, h4') || {}).textContent || 'Product';
                            title = clean(title);
                            if (title.length === 0) title = 'Product';
                            var img = parent.querySelector('img');
                            var imageUrl = getImgUrl(img);
                            var priceEl = parent.querySelector('.price, .product-price, .amount, [class*="price"]');
                            var price = clean(priceEl ? priceEl.textContent : '');
                            seen.add(href);
                            out.push(title.substring(0, 200) + '\\t' + href + '\\t' + imageUrl + '\\t' + price);
                        });
                    } catch (e2) {}
                }
                return out.join('\\n');
            })()
            """;
    }

    private List<ProductInfo> parseProductLines(String raw) {
        List<ProductInfo> list = new ArrayList<>();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            String title = parts.length > 0 ? parts[0] : "";
            String url = parts.length > 1 ? parts[1] : "";
            String imageUrl = parts.length > 2 ? parts[2] : "";
            String price = parts.length > 3 ? parts[3] : "";
            String addToCartLabel = parts.length > 4 ? parts[4].trim() : "";
            if (url.isBlank()) continue;
            if (addToCartLabel.isEmpty()) {
                list.add(ProductInfo.of(title, url, imageUrl, price));
            } else {
                list.add(ProductInfo.of(title, url, imageUrl, price, addToCartLabel));
            }
        }
        return list;
    }
}
