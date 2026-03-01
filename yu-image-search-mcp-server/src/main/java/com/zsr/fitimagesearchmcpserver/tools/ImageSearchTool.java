package com.zsr.fitimagesearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    @Value("${pexels.api-key:}")
    private String apiKey;

    // Pexels 常规搜索接口（请以文档为准）
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        if (StrUtil.isBlank(query)) {
            return "Error search image: query is blank";
        }
        if (StrUtil.isBlank(apiKey)) {
            return "Error search image: missing pexels.api-key";
        }
        try {
            List<String> images = searchMediumImages(query);
            if (images.isEmpty()) {
                return "No image found for query: " + query;
            }
            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     *
     * @param query
     * @return
     */
    public List<String> searchMediumImages(String query) {
        // 设置请求头（包含API密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        // 设置请求参数（仅包含query，可根据文档补充page、per_page等参数）
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        // 发送 GET 请求
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        JSONObject responseObj = JSONUtil.parseObj(response);
        JSONArray photos = responseObj.getJSONArray("photos");
        if (photos == null) {
            String message = responseObj.getStr("error");
            if (StrUtil.isBlank(message)) {
                message = responseObj.getStr("message");
            }
            if (StrUtil.isBlank(message)) {
                message = "missing photos field in response";
            }
            throw new IllegalStateException(message);
        }

        return photos
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
