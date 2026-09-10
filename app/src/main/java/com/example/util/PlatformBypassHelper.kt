package com.example.util

import android.webkit.CookieManager
import android.webkit.WebView

object PlatformBypassHelper {

    // Clean modern Android Chrome User-Agent matching official Android 14 Chrome browser
    // Prevents bot detection and eliminates "Sign in to confirm you're not a bot" on YouTube
    const val CHROME_MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36"

    /**
     * Pre-seeds cookies for YouTube, TikTok, Instagram, Facebook, Snapchat, and Bilibili
     * to prevent login walls, bot interstitials, and cookie consent barriers.
     */
    fun configureWebView(webView: WebView) {
        try {
            val ctx = webView.context
            val cacheRoot = java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(cacheRoot, "wasm").mkdirs()
            java.io.File(cacheRoot, "js").mkdirs()
            java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/index-dir").mkdirs()
        } catch (_: Exception) {}

        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)

            // YouTube: Seed verified consent and preference cookies so YouTube skips bot check & consent interstitials
            cookieManager.setCookie("https://m.youtube.com", "PREF=f4=4000000&hl=en&f6=40000000&f7=100; domain=.youtube.com; path=/; secure")
            cookieManager.setCookie("https://m.youtube.com", "SOCS=CAESEwgDEgk2OTcyMTY1NzQaAmVuIAEaBgiA_LyaBg; domain=.youtube.com; path=/; secure")
            cookieManager.setCookie("https://m.youtube.com", "CONSENT=YES+cb.20240310-09-p0.en+FX+111; domain=.youtube.com; path=/; secure")
            cookieManager.setCookie("https://m.youtube.com", "YSC=1; domain=.youtube.com; path=/; secure")

            // TikTok: Seed guest session cookie to allow direct browsing without login dialog
            cookieManager.setCookie("https://www.tiktok.com", "csrf_session_id=verified; domain=.tiktok.com; path=/; secure")
            cookieManager.setCookie("https://www.tiktok.com", "tt_webid_v2=guest_session; domain=.tiktok.com; path=/; secure")

            // Bilibili: Seed guest device cookies to prevent "Open in App" popups
            cookieManager.setCookie("https://m.bilibili.com", "buvid3=guest_buvid; domain=.bilibili.com; path=/; secure")
            cookieManager.setCookie("https://m.bilibili.com", "CURRENT_FNVAL=4048; domain=.bilibili.com; path=/; secure")

            cookieManager.flush()
        } catch (_: Exception) {}
    }

    /**
     * Comprehensive JavaScript injection that:
     * 1. Eliminates YouTube "Sign in to confirm you're not a bot" dialog and clicks 'Skip' automatically.
     * 2. Bypasses account/ID creation popups on TikTok, Facebook, Instagram, Snapchat, and Bilibili.
     * 3. Unlocks scrolling and auto-plays videos directly without forcing account creation or app install.
     */
    val BYPASS_LOGIN_JS = """
        (function() {
            try {
                // 0. Mask webdriver and spoof standard Chrome browser runtime
                try {
                    Object.defineProperty(navigator, 'webdriver', { get: () => false });
                    Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
                    Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
                    if (!window.chrome) {
                        window.chrome = { runtime: {}, loadTimes: function() {}, csi: function() {}, app: {} };
                    }
                } catch(e) {}

                // 1. Inject permanent stylesheet to kill all login, upsell, app-download & bot modals
                var styleId = 'newtube-permanent-bypass';
                if (!document.getElementById(styleId)) {
                    var style = document.createElement('style');
                    style.id = styleId;
                    style.innerHTML = `
                        /* Hide YouTube Logo from Mobile Web as explicitly requested by user */
                        #masthead-logo, ytm-mobile-topbar-renderer #header-bar .center,
                        [aria-label="YouTube"], a[href="/"] > svg,
                        ytm-home-logo, .mobile-topbar-header-endpoint,
                        .yt-spec-icon-badge-shape__icon, ytd-topbar-logo-renderer,
                        #header-bar .center, ytm-pivot-bar-item-renderer:first-child .pivot-bar-item-tab,
                        .ytm-logo, .icon-logo, ytm-mobile-topbar-renderer .header-logo,
                        a.header-logo, svg.header-logo-icon {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }

                        /* YouTube Bot Interstitial & Promo popups */
                        ytm-mealbar-promo-renderer,
                        ytm-upsell-dialog-renderer,
                        .promoted-sparkles-web-renderer,
                        .consent-bump-v2-lightbox,
                        #consent-bump,
                        div[class*="upsell"],
                        div[class*="dialog-container"]:has(button),
                        div[role="dialog"]:has([class*="sign-in" i]),
                        div[role="dialog"]:has([class*="login" i]) {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }

                        /* TikTok login modal, banner & forced app splash */
                        [class*="DivLoginModal"], [class*="ModalContainer"], [class*="login-modal"],
                        [class*="login-panel"], [id*="login-modal"], [class*="DivLoginContainer"],
                        [class*="DivBottomBannerContainer"], [class*="DivBannerContainer"],
                        [class*="FloatingComp"], [class*="DivMask"], [class*="ModalWrapper"],
                        [class*="DivAppDownloadContainer"], [class*="DivFeedAppDownloadBanner"],
                        .tiktok-modal, .login-modal-wrapper, div[data-e2e="login-modal"] {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }
                        
                        /* Instagram login popup & backdrop */
                        div[role="dialog"] div[class*="x1n2onr6"] div:has(button),
                        div._a3wf, div._ab37, div[class*="login-popup"],
                        div[class*="_a9-v"], div[class*="_a9--"],
                        div[class*="x78zum5"][class*="xdt5ytf"] > div[role="dialog"] {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }
                        
                        /* Facebook login barriers & watch login banner */
                        #login_popup_cta, #mobile_login_bar, [data-sigil="m-login-banner"],
                        ._5rut, ._95ke, div[id*="header-login-prompt"], div[data-sigil="login_prompt"] {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }

                        /* Snapchat auth overlays & login modals */
                        [class*="LoginModal"], [class*="SignupModal"], [class*="AuthOverlay"] {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }

                        /* Bilibili app prompts & login dialogs */
                        .m-openapp, .m-home-float-openapp, .launch-app-btn, .open-app-btn,
                        .app-link, div[class*="openapp"], .bili-dialog, .login-dialog,
                        .m-login, .passport-login-container, .v-dialog, .m-video-box .m-video-openapp,
                        .openapp-dialog, .launch-app-btn-wrap {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }
                        
                        /* Ensure free scrolling across all platforms */
                        html, body {
                            overflow: auto !important;
                            position: static !important;
                            height: auto !important;
                        }
                    `;
                    document.head.appendChild(style);
                }

                // 2. Active function to dismiss obstacles, bot dialogs, and auto-play video
                function dismissObstacles() {
                    try {
                        // A. Target YouTube 'Skip', 'Not now', and 'Continue on web' buttons
                        var clickables = document.querySelectorAll('button, a, div[role="button"], span, ytm-button-renderer');
                        for (var i = 0; i < clickables.length; i++) {
                            var el = clickables[i];
                            var text = (el.textContent || '').trim().toLowerCase();
                            if (text === 'skip' || text === 'not now' || text === 'continue on web' || text === 'use web version' || text === 'stay on web' || text === 'no thanks') {
                                try {
                                    el.click();
                                } catch(e) {}
                            }
                        }

                        // B. Target close icon buttons on TikTok, YouTube, Instagram & Bilibili
                        var closeSelectors = [
                            '[data-e2e="modal-close-icon"]',
                            'button[aria-label="Close"]',
                            'button[aria-label="close" i]',
                            'button[aria-label="Dismiss"]',
                            '.modal-close',
                            '[class*="close-icon"]',
                            '[class*="CloseButton"]',
                            '[aria-label*="skip" i]',
                            '.bili-dialog .close',
                            '.m-login-close',
                            '.btn-close',
                            'button:not([type="submit"]):has([aria-label*="close" i])'
                        ];
                        for (var j = 0; j < closeSelectors.length; j++) {
                            var btns = document.querySelectorAll(closeSelectors[j]);
                            for (var k = 0; k < btns.length; k++) {
                                try { btns[k].click(); } catch(e) {}
                            }
                        }

                        // C. Detect "Sign in to confirm you're not a bot" container and remove it immediately
                        var dialogs = document.querySelectorAll('div, section, [role="dialog"], ytm-upsell-dialog-renderer, ytm-mealbar-promo-renderer');
                        for (var d = 0; d < dialogs.length; d++) {
                            var dia = dialogs[d];
                            var diaText = (dia.innerText || dia.textContent || '').toLowerCase();
                            if (diaText.includes("not a bot") || diaText.includes("confirm you're not a bot") || diaText.includes("sign in to confirm")) {
                                var innerClicks = dia.querySelectorAll('button, a, span');
                                for (var ic = 0; ic < innerClicks.length; ic++) {
                                    var it = (innerClicks[ic].textContent || '').trim().toLowerCase();
                                    if (it === 'skip' || it === 'not now' || it === 'dismiss') {
                                        try { innerClicks[ic].click(); } catch(e) {}
                                    }
                                }
                                dia.remove();
                            }
                        }

                        // Remove backdrop scrims
                        var scrims = document.querySelectorAll('.modal-backdrop, .scrim, div[class*="backdrop"], div[class*="scrim"]');
                        for (var s = 0; s < scrims.length; s++) {
                            scrims[s].remove();
                        }

                        // D. Unmute and play ONLY the active video visible in viewport (prevents background buffering of other videos)
                        var vids = document.querySelectorAll('video');
                        var vh = window.innerHeight || 800;
                        for (var v = 0; v < vids.length; v++) {
                            var vid = vids[v];
                            var r = vid.getBoundingClientRect();
                            var isVisible = (r.top < vh && r.bottom > 0 && r.width > 0 && r.height > 0);
                            if (isVisible && vid.paused && vid.getAttribute('data-bypass-played') !== 'true') {
                                vid.setAttribute('data-bypass-played', 'true');
                                vid.play().catch(function(){});
                            }
                        }

                        // E. Force document scroll
                        if (document.body) {
                            document.body.style.overflow = 'auto';
                            document.body.style.position = 'static';
                        }
                    } catch(err) {}
                }

                // Run immediately
                dismissObstacles();

                // Periodic check for dynamic overlays (e.g. YouTube bot dialog or TikTok login mounted dynamically)
                var intervalCount = 0;
                var bypassInterval = setInterval(function() {
                    dismissObstacles();
                    intervalCount++;
                    if (intervalCount > 35) {
                        clearInterval(bypassInterval);
                    }
                }, 350);

                // MutationObserver for instant reaction as soon as dialog elements are mounted
                if (window.MutationObserver && !window._newtubeObserverAttached) {
                    window._newtubeObserverAttached = true;
                    var observer = new MutationObserver(function(mutations) {
                        dismissObstacles();
                    });
                    if (document.body) {
                        observer.observe(document.body, { childList: true, subtree: true });
                    }
                }
            } catch(e) {}
        })();
    """.trimIndent()

    /**
     * Injects the bypass script into the WebView.
     */
    fun injectBypass(webView: WebView?) {
        webView?.post {
            webView.evaluateJavascript(BYPASS_LOGIN_JS, null)
        }
    }
}

