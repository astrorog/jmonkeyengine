/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3tools.optimize;

import com.jme3.asset.AssetKey;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lukasz Bruun - lukasz.dk, normenhansen
 */
public class TextureAtlas {

    private static final Logger logger = Logger.getLogger(TextureAtlas.class.getName());
    private Map<String, byte[]> images;
    private int atlasWidth, atlasHeight;
    private Format format = Format.ABGR8;
    private Node root;
    private Map<String, TextureAtlasTile> locationMap;
    private String rootMapName;

    public TextureAtlas(int width, int height) {
        this.atlasWidth = width;
        this.atlasHeight = height;
        root = new Node(0, 0, width, height);
        locationMap = new TreeMap<String, TextureAtlasTile>();
    }

    /**
     * Add a texture for a specific map name
     * @param texture A texture to add to the atlas
     * @param mapName A freely chosen map name that can be later retrieved as a Texture. The first map name supplied will be the master map.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public boolean addTexture(Texture texture, String mapName) {
        if (texture == null) {
            throw new IllegalStateException("Texture cannot be null");
        }
        String name = textureName(texture);
        if (texture.getImage() != null && name != null) {
            return addImage(texture.getImage(), name, mapName, null);
        } else {
            throw new IllegalStateException("Source texture has no asset name");
        }
    }

    /**
     * Add a texture for a specific map name at the location of another existing texture (on the master map).
     * @param texture A texture to add to the atlas.
     * @param mapName A freely chosen map name that can be later retrieved as a Texture.
     * @param sourceTexture The base texture for determining the location.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public void addTexture(Texture texture, String mapName, Texture sourceTexture) {
        String sourceTextureName = textureName(sourceTexture);
        if (sourceTextureName == null) {
            throw new IllegalStateException("Source texture has no asset name");
        } else {
            addTexture(texture, mapName, sourceTextureName);
        }
    }

    /**
     * Add a texture for a specific map name at the location of another existing texture (on the master map).
     * @param texture A texture to add to the atlas.
     * @param mapName A freely chosen map name that can be later retrieved as a Texture.
     * @param sourceTextureName Name of the base texture for the location.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public void addTexture(Texture texture, String mapName, String sourceTextureName) {
        if (texture == null) {
            throw new IllegalStateException("Texture cannot be null");
        }
        String name = textureName(texture);
        if (texture.getImage() != null && name != null) {
            addImage(texture.getImage(), name, mapName, sourceTextureName);
        } else {
            throw new IllegalStateException("Texture has no asset name");
        }
    }

    private String textureName(Texture texture) {
        if (texture == null) {
            return null;
        }
        AssetKey key = texture.getKey();
        if (key != null) {
            return key.getName();
        } else {
            return null;
        }
    }

    private boolean addImage(Image image, String name, String mapName, String sourceTextureName) {
        if (rootMapName == null) {
            rootMapName = mapName;
        }
        if (sourceTextureName == null && !rootMapName.equals(mapName)) {
            throw new IllegalStateException("Cannot add texture to new map without source texture");
        }
        TextureAtlasTile location;
        if (sourceTextureName == null) {
            Node node = root.insert(image);
            if (node == null) {
                return false;
            }
            location = node.location;
        } else {
            location = locationMap.get(sourceTextureName);
            if (location == null) {
                throw new IllegalStateException("Cannot find location for source texture");
            }
        }
        locationMap.put(name, location);
        drawImage(image, location.getX(), location.getY(), mapName);
        return true;
    }

    private void drawImage(Image source, int x, int y, String mapName) {
        if (images == null) {
            images = new HashMap<String, byte[]>();
        }
        byte[] image = images.get(mapName);
        if (image == null) {
            image = new byte[atlasWidth * atlasHeight * 4];
            images.put(mapName, image);
        }
        //TODO: all buffers?
        ByteBuffer sourceData = source.getData(0);
        int height = source.getHeight();
        int width = source.getWidth();
        for (int yPos = 0; yPos < height; yPos++) {
            for (int xPos = 0; xPos < width; xPos++) {
                int i = ((xPos + x) + (yPos + y) * atlasWidth) * 4;
                if (source.getFormat() == Format.ABGR8) {
                    int j = (xPos + yPos * width) * 4;
                    image[i] = sourceData.get(j); //a
                    image[i + 1] = sourceData.get(j + 1); //b
                    image[i + 2] = sourceData.get(j + 2); //g
                    image[i + 3] = sourceData.get(j + 3); //r
                } else if (source.getFormat() == Format.BGR8) {
                    int j = (xPos + yPos * width) * 3;
                    image[i] = 0; //a
                    image[i + 1] = sourceData.get(j); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j + 2); //r
                } else if (source.getFormat() == Format.RGB8) {
                    int j = (xPos + yPos * width) * 3;
                    image[i] = 0; //a
                    image[i + 1] = sourceData.get(j + 2); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j); //r
                } else if (source.getFormat() == Format.RGBA8) {
                    int j = (xPos + yPos * width) * 4;
                    image[i] = sourceData.get(j + 3); //a
                    image[i + 1] = sourceData.get(j + 2); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j); //r
                } else {
                    logger.log(Level.WARNING, "Could not draw texture with format {0}", source.getFormat());
                }
            }
        }
    }

    /**
     * Get the <code>TextureAtlasTile</code> for the given Texture
     * @param texture The texture to retrieve the <code>TextureAtlasTile</code> for.
     * @return 
     */
    public TextureAtlasTile getAtlasTile(Texture texture) {
        String sourceTextureName = textureName(texture);
        if (sourceTextureName != null) {
            return getAtlasTile(sourceTextureName);
        }
        return null;
    }

    /**
     * Get the <code>TextureAtlasTile</code> for the given Texture
     * @param assetName The texture to retrieve the <code>TextureAtlasTile</code> for.
     * @return 
     */
    private TextureAtlasTile getAtlasTile(String assetName) {
        return locationMap.get(assetName);
    }

    /**
     * Gets a new atlas texture for the given map name.
     * @param mapName
     * @return 
     */
    public Texture getAtlasTexture(String mapName) {
        if (images == null) {
            return null;
        }
        byte[] image = images.get(mapName);
        if (image != null) {
            Texture2D tex = new Texture2D(new Image(format, atlasWidth, atlasHeight, BufferUtils.createByteBuffer(image)));
            tex.setMagFilter(Texture.MagFilter.Bilinear);
            tex.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
            tex.setWrap(Texture.WrapMode.Clamp);
            return tex;
        }
        return null;
    }

    private class Node {

        public TextureAtlasTile location;
        public Node child[];
        public Image image;

        public Node(int x, int y, int width, int height) {
            location = new TextureAtlasTile(x, y, width, height);
            child = new Node[2];
            child[0] = null;
            child[1] = null;
            image = null;
        }

        public boolean isLeaf() {
            return child[0] == null && child[1] == null;
        }

        // Algorithm from http://www.blackpawn.com/texts/lightmaps/
        public Node insert(Image image) {
            if (!isLeaf()) {
                Node newNode = child[0].insert(image);

                if (newNode != null) {
                    return newNode;
                }

                return child[1].insert(image);
            } else {
                if (this.image != null) {
                    return null; // occupied
                }

                if (image.getWidth() > location.getWidth() || image.getHeight() > location.getHeight()) {
                    return null; // does not fit
                }

                if (image.getWidth() == location.getWidth() && image.getHeight() == location.getHeight()) {
                    this.image = image; // perfect fit
                    return this;
                }

                int dw = location.getWidth() - image.getWidth();
                int dh = location.getHeight() - image.getHeight();

                if (dw > dh) {
                    child[0] = new Node(location.getX(), location.getY(), image.getWidth(), location.getHeight());
                    child[1] = new Node(location.getX() + image.getWidth(), location.getY(), location.getWidth() - image.getWidth(), location.getHeight());
                } else {
                    child[0] = new Node(location.getX(), location.getY(), location.getWidth(), image.getHeight());
                    child[1] = new Node(location.getX(), location.getY() + image.getHeight(), location.getWidth(), location.getHeight() - image.getHeight());
                }

                return child[0].insert(image);
            }
        }
    }

    public class TextureAtlasTile {

        private int x;
        private int y;
        private int width;
        private int height;

        public TextureAtlasTile(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * Get the transformed texture location for a given input location
         * @param previousLocation
         * @return 
         */
        public Vector2f getLocation(Vector2f previousLocation) {
            float x = (float) getX() / (float) atlasWidth;
            float y = (float) getY() / (float) atlasHeight;
            float w = (float) getWidth() / (float) atlasWidth;
            float h = (float) getHeight() / (float) atlasHeight;
            Vector2f location = new Vector2f(x, y);
            float prevX = previousLocation.x;
            float prevY = previousLocation.y;
            //TODO: remove workaround for texture wrap
            if (prevX > 1) {
                prevX = 1;
            }
            if (prevY > 1) {
                prevY = 1;
            }
            if (prevX < 0) {
                prevX = 0;
            }
            if (prevY < 0) {
                prevY = 0;
            }
            location.addLocal(prevX * w, prevY * h);
            return location;
        }

        /**
         * Transforms a whole texture coordinates buffer
         * @param inBuf The input texture buffer
         * @param offset The offset in the output buffer
         * @param outBuf The output buffer
         */
        public void transformTextureCoords(FloatBuffer inBuf, int offset, FloatBuffer outBuf) {
            Vector2f tex = new Vector2f();

            // offset is given in element units
            // convert to be in component units
            offset *= 2;

            for (int i = 0; i < inBuf.capacity() / 2; i++) {
                tex.x = inBuf.get(i * 2 + 0);
                tex.y = inBuf.get(i * 2 + 1);
                Vector2f location = getLocation(tex);
                //TODO: replace with proper texture wrapping for atlases..
                outBuf.put(offset + i * 2 + 0, location.x);
                outBuf.put(offset + i * 2 + 1, location.y);
            }
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}