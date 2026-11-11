package com.ferreteria.fc.domain.enums;


public enum ImageKind {

    USER_AVATAR(
            "users",
            256,
            2 * 1024 * 1024,
            true
    ),

    PRODUCT_IMAGE(
            "products",
            1200,
            5 * 1024 * 1024,
            false
    );

    private final String folder;
    private final int maxDimension;
    private final long maxBytes;
    private final boolean square;

    ImageKind(
            String folder,
            int maxDimension,
            long maxBytes,
            boolean square
    ) {
        this.folder = folder;
        this.maxDimension = maxDimension;
        this.maxBytes = maxBytes;
        this.square = square;
    }

    public String getFolder() {
        return folder;
    }

    public int getMaxDimension() {
        return maxDimension;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public boolean isSquare() {
        return square;
    }

}
