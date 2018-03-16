package com.novoda.noplayer.internal.exoplayer;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.novoda.noplayer.NoPlayer;

import java.nio.ByteBuffer;

class FramesPerSecondReportingMediaCodecVideoRenderer extends MediaCodecVideoRenderer {

    private final FramesPerSecondCalculator framesPerSecondCalculator;
    private final NoPlayer.FramesPerSecondChangedListener framesPerSecondChangedListeners;
    @Nullable
    private final Handler eventHandler;

    private boolean hasDroppedOutputBuffer;
    private boolean shouldSkip;

    @SuppressWarnings({"checkstyle:parameternumber", "PMD.ExcessiveParameterList"})
    FramesPerSecondReportingMediaCodecVideoRenderer(Context context,
                                                    MediaCodecSelector mediaCodecSelector,
                                                    long allowedJoiningTimeMs,
                                                    @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                                    boolean playClearSamplesWithoutKeys,
                                                    @Nullable Handler eventHandler,
                                                    @Nullable VideoRendererEventListener eventListener,
                                                    int maxDroppedFramesToNotify,
                                                    FramesPerSecondCalculator framesPerSecondCalculator,
                                                    NoPlayer.FramesPerSecondChangedListener framesPerSecondChangedListeners) {
        super(
                context,
                mediaCodecSelector,
                allowedJoiningTimeMs,
                drmSessionManager,
                playClearSamplesWithoutKeys,
                eventHandler,
                eventListener,
                maxDroppedFramesToNotify
        );

        this.eventHandler = eventHandler;
        this.framesPerSecondCalculator = framesPerSecondCalculator;
        this.framesPerSecondChangedListeners = framesPerSecondChangedListeners;
    }

    @SuppressWarnings({"checkstyle:parameternumber", "PMD.ExcessiveParameterList"})
    @Override
    protected boolean processOutputBuffer(long positionUs,
                                          long elapsedRealtimeUs,
                                          MediaCodec codec,
                                          ByteBuffer buffer,
                                          int bufferIndex,
                                          int bufferFlags,
                                          long bufferPresentationTimeUs,
                                          boolean shouldSkip) throws ExoPlaybackException {
        this.shouldSkip = shouldSkip;
        return super.processOutputBuffer(
                positionUs,
                elapsedRealtimeUs,
                codec,
                buffer,
                bufferIndex,
                bufferFlags,
                bufferPresentationTimeUs,
                shouldSkip
        );
    }

    @Override
    protected void onProcessedOutputBuffer(final long presentationTimeUs) {
        super.onProcessedOutputBuffer(presentationTimeUs);
        if (hasDroppedOutputBuffer || shouldSkip) {
            return;
        }

        final int fps = framesPerSecondCalculator.calculate(presentationTimeUs);

        if (eventHandler != null) {
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    framesPerSecondChangedListeners.onFramesPerSecondChanged(fps);
                }
            });
        }
    }

    @Override
    protected boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) {
        hasDroppedOutputBuffer = super.shouldDropOutputBuffer(earlyUs, elapsedRealtimeUs);
        return hasDroppedOutputBuffer;
    }
}