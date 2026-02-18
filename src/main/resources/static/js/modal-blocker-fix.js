/**
 * Modal Blocker Fix - Prevents invisible modals/overlays from blocking interaction
 * Runs on every page load to ensure clean state
 */

(function() {
    'use strict';

    function removeModalBlockers() {
        console.log('🔧 Modal Blocker Fix: Checking for blocking elements...');

        // 1. Remove all modal-related elements
        const blockingSelectors = [
            '.modal-backdrop',
            '.modal.show',
            '[role="dialog"][style*="display: block"]',
            '[class*="overlay"]',
            '[class*="backdrop"]'
        ];

        let removed = 0;
        blockingSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => {
                el.remove();
                removed++;
            });
        });

        // 2. Clean body classes and styles
        document.body.classList.remove('modal-open');
        if (document.body.style.overflow === 'hidden') {
            document.body.style.overflow = '';
        }
        if (document.body.style.paddingRight) {
            document.body.style.paddingRight = '';
        }

        // 3. Reset pointer events if blocked
        if (document.body.style.pointerEvents === 'none') {
            document.body.style.pointerEvents = 'auto';
        }

        if (removed > 0) {
            console.log(`✅ Removed ${removed} blocking element(s)`);
        }
    }

    // Run immediately when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', removeModalBlockers);
    } else {
        removeModalBlockers();
    }

    // Run again after page fully loads (catches late-loaded elements)
    window.addEventListener('load', function() {
        setTimeout(removeModalBlockers, 100);
    });

    // Also run when visibility changes (e.g., returning to tab)
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            removeModalBlockers();
        }
    });

    console.log('✅ Modal Blocker Fix installed');
})();
