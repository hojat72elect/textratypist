/*
 * Copyright (c) 2024 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tommyettinger.textra;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

/**
 * A subclass of {@link Skin} that includes a serializer for Structured JSON Fonts, which are typically generated by
 * <a href="https://github.com/tommyettinger/fontwriter">fontwriter</a> (hence the "FW" in the name). This can load
 * FontWriter's JSON format into either {@link Font} and {@link BitmapFont} objects, from the same files. It can also
 * load {@link Font}s from AngelCode BMFont files (with a ".fnt" extension), and continues to be able to load
 * {@link BitmapFont}s from those files as well.
 * <br>
 * If you are using {@link com.badlogic.gdx.assets.AssetManager}, use {@link FWSkinLoader}.
 */

public class FWSkin extends Skin {
    /** Creates an empty skin. */
    public FWSkin() {
    }
    
    /** Creates a skin containing the resources in the specified skin JSON file. If a file in the same directory with a ".atlas"
     * extension exists, it is loaded as a {@link TextureAtlas} and the texture regions added to the skin. The atlas is
     * automatically disposed when the skin is disposed.
     * @param  skinFile The JSON file to be read.
     */
    public FWSkin(FileHandle skinFile) {
        super(skinFile);
        
    }
    
    /** Creates a skin containing the resources in the specified skin JSON file and the texture regions from the specified atlas.
     * The atlas is automatically disposed when the skin is disposed.
     * @param skinFile The JSON file to be read.
     * @param atlas The texture atlas to be associated with the {@link Skin}.
     */
    public FWSkin(FileHandle skinFile, TextureAtlas atlas) {
        super(skinFile, atlas);
    }
    
    /** Creates a skin containing the texture regions from the specified atlas. The atlas is automatically disposed when the skin
     * is disposed.
     * @param atlas The texture atlas to be associated with the {@link Skin}.
     */
    public FWSkin(TextureAtlas atlas) {
        super(atlas);
    }
    
    /**
     * Overrides the default JSON loader to process Structured JSON Fonts from a Skin JSON.
     * This allows Font and BitmapFont items to be loaded from either .fnt or .json files.
     * @param skinFile The JSON file to be processed.
     * @return The {@link Json} used to read the file.
     */
    @Override
    protected Json getJsonLoader(final FileHandle skinFile) {
        Json json = super.getJsonLoader(skinFile);
        final Skin skin = this;

        json.setSerializer(Font.class, new Json.ReadOnlySerializer<Font>() {
            @Override
            public Font read(Json json, JsonValue jsonData, Class type) {
                String path = json.readValue("file", String.class, jsonData);

                FileHandle fontFile = skinFile.sibling(path);
                if (!fontFile.exists()) fontFile = Gdx.files.internal(path);
                if (!fontFile.exists()) throw new SerializationException("Font file not found: " + fontFile);

                path = fontFile.path();

                boolean lzb = path.endsWith(".dat");
                boolean fw = path.endsWith(".json");
                float scaledSize = json.readValue("scaledSize", float.class, -1f, jsonData);
                float xAdjust = json.readValue("xAdjust", float.class, 0f, jsonData);
                float yAdjust = json.readValue("yAdjust", float.class, 0f, jsonData);
                float widthAdjust = json.readValue("widthAdjust", float.class, 0f, jsonData);
                float heightAdjust = json.readValue("heightAdjust", float.class, 0f, jsonData);
                Boolean useIntegerPositions = json.readValue("useIntegerPositions", Boolean.class, false, jsonData);
                Boolean makeGridGlyphs = json.readValue("makeGridGlyphs", Boolean.class, true, jsonData);


                // Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
                String regionName = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'))+1, path.lastIndexOf('.'));
                try {
                    Font font;
                    Array<TextureRegion> regions = skin.getRegions(regionName);
                    if (regions != null && regions.notEmpty()) {
                        if(fw || lzb)
                            font = new Font(fontFile, regions.first(), xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                        else
                            font = new Font(path, regions, Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                    } else {
                        TextureRegion region = skin.optional(regionName, TextureRegion.class);
                        if (region != null)
                        {
                            if(fw || lzb)
                                font = new Font(fontFile, region, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                            else
                                font = new Font(path, region, Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                        }
                        else {
                            FileHandle imageFile = Gdx.files.internal(path).sibling(regionName + ".png");
                            if (imageFile.exists()) {
                                if(fw || lzb)
                                    font = new Font(fontFile, new TextureRegion(new Texture(imageFile)), xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                                else
                                    font = new Font(path, new TextureRegion(new Texture(imageFile)), Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                            } else {
                                if(fw || lzb)
                                    throw new RuntimeException("Missing image file or TextureRegion.");
                                else
                                    font = new Font(path);
                            }
                        }
                    }
                    font.useIntegerPositions(useIntegerPositions);
                    // Scaled size is the desired cap height to scale the font to.
                    if (scaledSize != -1) font.scaleHeightTo(scaledSize);
                    return font;
                } catch (RuntimeException ex) {
                    throw new SerializationException("Error loading Font: " + path, ex);
                }
            }
        });

        json.setSerializer(BitmapFont.class, new Json.ReadOnlySerializer<BitmapFont>() {
            public BitmapFont read (Json json, JsonValue jsonData, Class type) {
                String path = json.readValue("file", String.class, jsonData);

                FileHandle fontFile = skinFile.sibling(path);
                if (!fontFile.exists()) fontFile = Gdx.files.internal(path);
                if (!fontFile.exists()) throw new SerializationException("Font file not found: " + fontFile);

                boolean lzb = "dat".equalsIgnoreCase(fontFile.extension());
                boolean fw = "json".equalsIgnoreCase(fontFile.extension());

                float scaledSize = json.readValue("scaledSize", float.class, -1f, jsonData);
                Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
                Boolean markupEnabled = json.readValue("markupEnabled", Boolean.class, false, jsonData);
                // This defaults to true if loading from .fnt, or false if loading from .json :
                Boolean useIntegerPositions = json.readValue("useIntegerPositions", Boolean.class, !(fw || lzb), jsonData);
                float xAdjust = json.readValue("xAdjust", float.class, 0f, jsonData);
                float yAdjust = json.readValue("yAdjust", float.class, 0f, jsonData);
                float widthAdjust = json.readValue("widthAdjust", float.class, 0f, jsonData);
                float heightAdjust = json.readValue("heightAdjust", float.class, 0f, jsonData);
                Boolean makeGridGlyphs = json.readValue("makeGridGlyphs", Boolean.class, true, jsonData);

                // Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
                String regionName = fontFile.nameWithoutExtension();
                try {
                    BitmapFont bitmapFont;
                    Font font;
                    Array<TextureRegion> regions = skin.getRegions(regionName);
                    if (regions != null && regions.notEmpty()) {
                        if(fw || lzb) {
                            bitmapFont = BitmapFontSupport.loadStructuredJson(fontFile, regions.first(), flip);
                            font = new Font(fontFile, regions.first(), xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                        }
                        else {
                            bitmapFont = new BitmapFont(new BitmapFont.BitmapFontData(fontFile, flip), regions, true);
                            font = new Font(fontFile, regions, Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                        }
                    } else {
                        TextureRegion region = skin.optional(regionName, TextureRegion.class);
                        if (region != null)
                        {
                            if(fw || lzb) {
                                bitmapFont = BitmapFontSupport.loadStructuredJson(fontFile, region, flip);
                                font = new Font(fontFile, region, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                            }
                            else {
                                bitmapFont = new BitmapFont(fontFile, region, flip);
                                font = new Font(fontFile, region, Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                            }
                        }
                        else {
                            FileHandle imageFile = fontFile.sibling(regionName + ".png");
                            if (imageFile.exists()) {
                                region = new TextureRegion(new Texture(imageFile));
                                if(fw || lzb) {
                                    bitmapFont = BitmapFontSupport.loadStructuredJson(fontFile, region, flip);
                                    font = new Font(fontFile, region, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs, true);
                                } else {
                                    bitmapFont = new BitmapFont(fontFile, region, flip);
                                    font = new Font(path, region, Font.DistanceFieldType.STANDARD, xAdjust, yAdjust, widthAdjust, heightAdjust, makeGridGlyphs);
                                }
                            } else {
                                if(fw || lzb)
                                    throw new RuntimeException("Missing image file or TextureRegion.");
                                else {
                                    bitmapFont = new BitmapFont(fontFile, flip);
                                    font = new Font(path);
                                }
                            }
                        }
                    }
                    bitmapFont.getData().markupEnabled = markupEnabled;
                    bitmapFont.setUseIntegerPositions(useIntegerPositions);
                    font.useIntegerPositions(useIntegerPositions);
                    // Scaled size is the desired cap height to scale the font to.
                    if (scaledSize != -1) {
                        bitmapFont.getData().setScale(scaledSize / bitmapFont.getCapHeight());
                        font.scaleHeightTo(scaledSize);
                    }

                    skin.add(jsonData.name, font, Font.class);

                    return bitmapFont;
                } catch (RuntimeException ex) {
                    throw new SerializationException("Error loading BitmapFont: " + fontFile, ex);
                }
            }
        });
        
        json.setSerializer(Label.LabelStyle.class, new Json.ReadOnlySerializer<Label.LabelStyle>() {
            @Override
            public Label.LabelStyle read(Json json, JsonValue jsonData, Class type) {
                Label.LabelStyle s2d = new Label.LabelStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.LabelStyle(skin.get(json.readValue("font", String.class, jsonData), Font.class), s2d.fontColor),
                        Styles.LabelStyle.class);
                return s2d;
            }
        });

        json.setSerializer(TextButton.TextButtonStyle.class, new Json.ReadOnlySerializer<TextButton.TextButtonStyle>() {
            @Override
            public TextButton.TextButtonStyle read(Json json, JsonValue jsonData, Class type) {
                TextButton.TextButtonStyle s2d = new TextButton.TextButtonStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.TextButtonStyle(s2d.up, s2d.down, s2d.checked,
                        skin.get(json.readValue("font", String.class, jsonData), Font.class)), Styles.TextButtonStyle.class);
                return s2d;
            }
        });

        json.setSerializer(ImageTextButton.ImageTextButtonStyle.class, new Json.ReadOnlySerializer<ImageTextButton.ImageTextButtonStyle>() {
            @Override
            public ImageTextButton.ImageTextButtonStyle read(Json json, JsonValue jsonData, Class type) {
                ImageTextButton.ImageTextButtonStyle s2d = new ImageTextButton.ImageTextButtonStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.ImageTextButtonStyle(s2d.up, s2d.down, s2d.checked,
                        skin.get(json.readValue("font", String.class, jsonData), Font.class)), Styles.ImageTextButtonStyle.class);
                return s2d;
            }
        });

        json.setSerializer(CheckBox.CheckBoxStyle.class, new Json.ReadOnlySerializer<CheckBox.CheckBoxStyle>() {
            @Override
            public CheckBox.CheckBoxStyle read(Json json, JsonValue jsonData, Class type) {
                CheckBox.CheckBoxStyle s2d = new CheckBox.CheckBoxStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.CheckBoxStyle(s2d.checkboxOff, s2d.checkboxOn,
                        skin.get(json.readValue("font", String.class, jsonData), Font.class), s2d.fontColor), Styles.CheckBoxStyle.class);
                return s2d;
            }
        });
        
        json.setSerializer(Window.WindowStyle.class, new Json.ReadOnlySerializer<Window.WindowStyle>() {
            @Override
            public Window.WindowStyle read(Json json, JsonValue jsonData, Class type) {
                Window.WindowStyle s2d = new Window.WindowStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.WindowStyle(skin.get(json.readValue("titleFont", String.class, jsonData), Font.class),
                        s2d.titleFontColor, s2d.background), Styles.WindowStyle.class);
                return s2d;
            }
        });

        
        json.setSerializer(TextTooltip.TextTooltipStyle.class, new Json.ReadOnlySerializer<TextTooltip.TextTooltipStyle>() {
            @Override
            public TextTooltip.TextTooltipStyle read(Json json, JsonValue jsonData, Class type) {
                TextTooltip.TextTooltipStyle s2d = new TextTooltip.TextTooltipStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.TextTooltipStyle(skin.get(json.readValue("label", String.class, jsonData),
                        Styles.LabelStyle.class),s2d.background), Styles.TextTooltipStyle.class);
                return s2d;
            }
        });

        json.setSerializer(List.ListStyle.class, new Json.ReadOnlySerializer<List.ListStyle>() {
            @Override
            public List.ListStyle read(Json json, JsonValue jsonData, Class type) {
                List.ListStyle s2d = new List.ListStyle();
                json.readFields(s2d, jsonData);
                skin.add(jsonData.name, new Styles.ListStyle(skin.get(json.readValue("font", String.class, jsonData), Font.class),
                        s2d.fontColorSelected, s2d.fontColorUnselected, s2d.background), Styles.ListStyle.class);
                return s2d;
            }
        });

        return json;
    }
}