package com.github.lumin.graphics.text;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.github.lumin.graphics.text.ttf.TtfFontLoader;

public class StaticFontLoader {

    public static final TtfFontLoader DEFAULT = new TtfFontLoader(ResourceLocationUtils.getIdentifier("fonts/ducksans.ttf"));

    public static final TtfFontLoader REGULAR = new TtfFontLoader(ResourceLocationUtils.getIdentifier("fonts/regular.ttf"));

    public static final TtfFontLoader ICONS = new TtfFontLoader(ResourceLocationUtils.getIdentifier("fonts/icon.ttf"));

}
