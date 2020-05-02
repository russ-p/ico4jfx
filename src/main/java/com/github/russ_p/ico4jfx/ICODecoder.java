package com.github.russ_p.ico4jfx;

import java.io.IOException;
import java.util.List;

import com.github.russ_p.ico4jfx.image4j.ColorEntry;
import com.github.russ_p.ico4jfx.image4j.IconEntry;
import com.github.russ_p.ico4jfx.image4j.InfoHeader;
import com.github.russ_p.ico4jfx.image4j.LittleEndianInputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * @author ruslan
 * 
 *         Based on image4j ICODecoder by Ian McDonagh
 */
public class ICODecoder {

	private static final int PNG_MAGIC = 0x89504E47;
	private static final int PNG_MAGIC_LE = 0x474E5089;
	private static final int PNG_MAGIC2 = 0x0D0A1A0A;
	private static final int PNG_MAGIC2_LE = 0x0A1A0A0D;

	@SuppressWarnings("unused")
	public static List<Image> read(java.io.InputStream is) throws IOException {
		LittleEndianInputStream in = new LittleEndianInputStream(is);

		// Reserved 2 byte = 0
		short sReserved = in.readShortLE();
		// Type 2 byte = 1
		short sType = in.readShortLE();
		// Count 2 byte Number of Icons in this file
		short sCount = in.readShortLE();

		// Entries Count * 16 list of icons
		IconEntry[] entries = new IconEntry[sCount];
		for (short s = 0; s < sCount; s++) {
			entries[s] = new IconEntry(in);
		}

		int i = 0;
		// images list of bitmap structures in BMP format
		java.util.List<Image> ret = new java.util.ArrayList<>(sCount);

		try {
			for (i = 0; i < sCount; i++) {

				int info = in.readIntLE();
				if (info == 40) {

					// read XOR bitmap
					// BMPDecoder bmp = new BMPDecoder(is);
					InfoHeader infoHeader = BMPDecoder.readInfoHeader(in, info);
					InfoHeader andHeader = new InfoHeader(infoHeader);
					andHeader.iHeight = (int) (infoHeader.iHeight / 2);
					InfoHeader xorHeader = new InfoHeader(infoHeader);
					xorHeader.iHeight = andHeader.iHeight;

					andHeader.sBitCount = 1;
					andHeader.iNumColors = 2;

					// for now, just read all the raster data (xor + and)
					// and store as separate images

					Image xor = BMPDecoder.read(xorHeader, in);

					WritableImage wimg = new WritableImage(xorHeader.iWidth, xorHeader.iHeight);

					ColorEntry[] andColorTable = new ColorEntry[] {
							new ColorEntry(255, 255, 255, 255),
							new ColorEntry(0, 0, 0, 0)
					};

					if (infoHeader.sBitCount == 32) {
						// transparency from alpha
						// ignore bytes after XOR bitmap
						int size = entries[i].iSizeInBytes;
						int infoHeaderSize = infoHeader.iSize;
						// data size = w * h * 4
						int dataSize = xorHeader.iWidth * xorHeader.iHeight * 4;
						int skip = size - infoHeaderSize - dataSize;

						// ignore AND bitmap since alpha channel stores transparency
						int skipped = in.skipBytes(skip);
						int s = skip;
						while (skipped < s) {
							if (skipped < 0) {
								throw new IOException("Failed to read [skip]");
							}
							s = skip - skipped;
							skipped = in.skipBytes(s);
						}
						// просто копирование (так было в оригинале)
						PixelReader src = xor.getPixelReader();
						PixelWriter target = wimg.getPixelWriter();
						for (int y = 0; y < xorHeader.iHeight; y++) {
							for (int x = 0; x < xorHeader.iWidth; x++) {
								target.setColor(x, y, src.getColor(x, y));
							}
						}

					} else {
						Image and = BMPDecoder.read(andHeader, in, andColorTable);

						PixelReader src = xor.getPixelReader();
						PixelReader srcAlpha = and.getPixelReader();
						PixelWriter target = wimg.getPixelWriter();

						for (int y = 0; y < xorHeader.iHeight; y++) {
							for (int x = 0; x < xorHeader.iWidth; x++) {
								Color c = src.getColor(x, y);
								double a = srcAlpha.getArgb(x, y) == -1 ? 1d : 0d;
								target.setColor(x, y, new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
							}
						}
					}
					ret.add(wimg);// TODO:
				}
				// check for PNG magic header and that image height and width = 0 = 256 -> Vista
				// format
				else if (info == PNG_MAGIC_LE) {

					int info2 = in.readIntLE();

					if (info2 != PNG_MAGIC2_LE) {
						throw new IOException("Unrecognized icon format for image #" + i);
					}

					IconEntry e = entries[i];
					byte[] pngData = new byte[e.iSizeInBytes - 8];
					int count = in.read(pngData);
					if (count != pngData.length) {
						throw new IOException("Unable to read image #" + i + " - incomplete PNG compressed data");
					}
					java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
					java.io.DataOutputStream dout = new java.io.DataOutputStream(bout);
					dout.writeInt(PNG_MAGIC);
					dout.writeInt(PNG_MAGIC2);
					dout.write(pngData);
					byte[] pngData2 = bout.toByteArray();
					java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(pngData2);

					Image png = new Image(bin);
					if (png != null && png.getException() == null) {
						ret.add(png);
					}
				} else {
					throw new IOException("Unrecognized icon format for image #" + i);
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new IOException("Failed to read image # " + i);
		}

		return ret;
	}

}
