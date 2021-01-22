package com.android.jesse.plugin;

import android.text.Html;
import android.text.Spanned;

public class HtmlUtil {

    public static Spanned fromHtml(String source) {
        if (null == source) {
            source = "";
        }
        return Html.fromHtml(source);
    }
}
