package com.instakill.media.processing;

/**
 * Turns an uploaded image into the version that is actually stored.
 *
 * <p>This is deliberately a pure bytes-in/bytes-out contract with no web or storage types, so the implementation can
 * later move behind a message queue or a background worker without touching its callers.
 */
public interface ImageProcessor {

    /** Whether images with this file extension (lower case, no dot) can be processed. */
    boolean supports(String extension);

    /**
     * @param source    the original uploaded bytes
     * @param extension the file extension of the upload, which also selects the output format
     * @return the processed image encoded in the same format
     * @throws ImageProcessingException if the bytes are not a readable image
     */
    byte[] process(byte[] source, String extension);
}
