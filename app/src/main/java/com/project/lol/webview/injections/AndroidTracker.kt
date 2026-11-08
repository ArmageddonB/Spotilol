package com.project.lol.webview.injections

object AndroidTracker {
    const val CONTENT = """

            /*
             * Reads the now-playing info. The page elements are tried first (old and new
             * names), then Spotify's own Media Session metadata, then the page title.
             * So a renamed element no longer leaves the notification / Bluetooth / player
             * without title, artist or cover.
             */
            window.splReadNowPlaying = function(){
                var out = { track: null, artist: '', cover: null, album: '' };
                function q(sel){ try{ return document.querySelector(sel); }catch(e){ return null; } }
                function txt(el){ return el ? ((el.textContent || '').trim()) : ''; }
                var ms = null;
                try{
                    var md = navigator.mediaSession && navigator.mediaSession.metadata;
                    if(md && (md.title || md.artist)) ms = md;
                }catch(e){}
                var w = q('[data-testid="now-playing-widget"]');

                var title = txt(q('a[data-testid=context-item-link]'))
                    || txt(q('[data-testid=context-item-info-title]'));
                if(!title && w) title = txt(w.querySelector('a[href*="/track/"], a[href*="/episode/"]'));
                if(!title && ms) title = (ms.title || '').trim();

                /* The media session can lag behind a track change: trust it only for the same title. */
                var msOk = !!ms && (!title || String(ms.title || '').trim().toLowerCase() === title.trim().toLowerCase());

                var artist = txt(q('a[data-testid=context-item-info-artist]'))
                    || txt(q('a[data-testid=context-item-info-show]'))
                    || txt(q('[data-testid=context-item-info-subtitles]'));
                if(!artist && w){
                    var al = w.querySelectorAll('a[href*="/artist/"], a[href*="/show/"]'), an = [];
                    for(var ai = 0; ai < al.length; ai++){ var at = txt(al[ai]); if(at && an.indexOf(at) < 0) an.push(at); }
                    artist = an.join(', ');
                }
                if(!artist){
                    /* Artist links next to the title in the now-playing bar, whatever their test ids are. */
                    var tl0 = q('a[data-testid=context-item-link]') || q('[data-testid=context-item-info-title]');
                    for(var up = tl0, lv = 0; up && lv < 6 && !artist; up = up.parentElement, lv++){
                        var nl = up.querySelectorAll ? up.querySelectorAll('a[href*="/artist/"], a[href*="/show/"]') : [];
                        if(nl.length && nl.length <= 10){
                            var nn = [];
                            for(var ni = 0; ni < nl.length; ni++){ var nt = txt(nl[ni]); if(nt && nn.indexOf(nt) < 0) nn.push(nt); }
                            artist = nn.join(', ');
                        }
                        if(nl.length > 10) break;
                    }
                }
                if(!artist && msOk) artist = (ms.artist || '').trim();

                if(!title){
                    var dt = (document.title || '').split(' • ');
                    if(dt.length >= 2 && dt[0].trim() && dt[1].trim().indexOf('Spotify') !== 0){
                        title = dt[0].trim();
                        if(!artist) artist = dt[1].trim();
                    }
                }

                var cover = null;
                var im = q('img[data-testid=cover-art-image]');
                if(!im && w) im = w.querySelector('img[src]');
                if(im && im.src) cover = im.src;
                if(!cover && msOk && ms.artwork && ms.artwork.length){
                    var best = null, bestSize = -1;
                    for(var i = 0; i < ms.artwork.length; i++){
                        var a = ms.artwork[i];
                        if(!a || !a.src) continue;
                        var sz = parseInt(String(a.sizes || '').split('x')[0], 10) || 0;
                        if(sz >= bestSize){ best = a.src; bestSize = sz; }
                    }
                    cover = best;
                }
                if(cover && cover.indexOf('i.scdn.co') !== -1){
                    cover = cover.replace(/ab67616d0000[0-9a-f]{4}/, 'ab67616d000082c1');
                    cover = cover.replace(/ab6761670000[0-9a-f]{4}/, 'ab676167000082e8');
                }

                out.track = title || null;
                out.artist = artist || '';
                out.cover = cover || null;
                out.album = (msOk && ms.album) ? String(ms.album).trim() : '';
                if(typeof window.splFillNowPlaying === 'function'){ try{ window.splFillNowPlaying(out); }catch(e){} }
                return out;
            };

            window.addAndAuto = function(){
                if(aaint) {
                    if(typeof aaint.disconnect==='function') { try { aaint.disconnect(); } catch(e){} }
                    else { clearInterval(aaint); }
                }
                function readTrackState(){
                    var np = window.splReadNowPlaying();
                    track = np.track;
                    artist = np.artist;
                    window.__splMsAlbum = np.album;
                    var rr = document.querySelector('button[data-testid=control-button-repeat]');
                    if(rr) repmode=rr.getAttribute('aria-checked'); else repmode='false';
                    shuffle = (typeof window.splShuffleState === 'function') ? window.splShuffleState() : 'off';
                    var fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
                    if(fb && fb.getAttribute('aria-checked')==='true') isfav=true; else isfav=false;
                    playing=window.splIsPlayingSticky();
                    var rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
                    if(rg) { duration=parseInt(rg.getAttribute('max')); position=parseInt(rg.getAttribute('value')); }
                    else { duration=null; position=null; }
                    cover = np.cover;
                    updMedia();
                }
                try {
                    var npTarget = document.querySelector('aside[data-testid="now-playing-bar"]') || document.body;
                    var readTimeout;
                    var obs = new MutationObserver(function(){
                        clearTimeout(readTimeout);
                        readTimeout = setTimeout(readTrackState, 200);
                    });
                    obs.observe(npTarget, { childList: true, subtree: true, attributes: true, characterData: true });
                    aaint = obs;
                    readTrackState();
                } catch(e) {
                    aaint = setInterval(readTrackState, 1000);
                }
            };
        
    """
}
