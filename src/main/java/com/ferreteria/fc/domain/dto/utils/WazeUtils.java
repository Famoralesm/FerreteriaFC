package com.ferreteria.fc.domain.dto.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WazeUtils {

    private WazeUtils() {}

    public static String generateLink(String address) {
        return "https://waze.com/ul?q=" +
                URLEncoder.encode(address, StandardCharsets.UTF_8);
    }
}
