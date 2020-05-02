package com.github.russ_p.ico4jfx;

import java.io.IOException;

import com.github.russ_p.ico4jfx.image4j.BMPConstants;
import com.github.russ_p.ico4jfx.image4j.ColorEntry;
import com.github.russ_p.ico4jfx.image4j.InfoHeader;
import com.github.russ_p.ico4jfx.image4j.LittleEndianInputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Decodes images in BMP format.
 * 
 * original @author Ian McDonagh
 */
@SuppressWarnings("unused")
class BMPDecoder {

	private Image img;
	private InfoHeader infoHeader;

	/**
	 * Creates a new instance of BMPDecoder and reads the BMP data from the source.
	 * 
	 * @param in the source <tt>InputStream<tt> from which to read the BMP data
	 * @throws java.io.IOException if an error occurs
	 */
	public BMPDecoder(java.io.InputStream in) throws IOException {
		LittleEndianInputStream lis = new LittleEndianInputStream(in);

		/* header [14] */

		// signature "BM" [2]
		byte[] bsignature = new byte[2];
		lis.read(bsignature);
		String signature = new String(bsignature, "UTF-8");

		if (!signature.equals("BM")) {
			throw new IOException("Invalid signature '" + signature + "' for BMP format");
		}

		// file size [4]
		int fileSize = lis.readIntLE();

		// reserved = 0 [4]
		int reserved = lis.readIntLE();

		// DataOffset [4] file offset to raster data
		int dataOffset = lis.readIntLE();

		/* info header [40] */

		infoHeader = readInfoHeader(lis);

		/* Color table and Raster data */

		img = read(infoHeader, lis);
	}

	/**
	 * Retrieves a bit from the lowest order byte of the given integer.
	 * 
	 * @param bits  the source integer, treated as an unsigned byte
	 * @param index the index of the bit to retrieve, which must be in the range
	 *              <tt>0..7</tt>.
	 * @return the bit at the specified index, which will be either <tt>0</tt> or
	 *         <tt>1</tt>.
	 */
	private static int getBit(int bits, int index) {
		return (bits >> (7 - index)) & 1;
	}

	/**
	 * Retrieves a nibble (4 bits) from the lowest order byte of the given integer.
	 * 
	 * @param nibbles the source integer, treated as an unsigned byte
	 * @param index   the index of the nibble to retrieve, which must be in the
	 *                range <tt>0..1</tt>.
	 * @return the nibble at the specified index, as an unsigned byte.
	 */
	private static int getNibble(int nibbles, int index) {
		return (nibbles >> (4 * (1 - index))) & 0xF;
	}

	/**
	 * The <tt>InfoHeader</tt> structure, which provides information about the BMP
	 * data.
	 * 
	 * @return the <tt>InfoHeader</tt> structure that was read from the source data
	 *         when this <tt>BMPDecoder</tt> was created.
	 */
	public InfoHeader getInfoHeader() {
		return infoHeader;
	}

	/**
	 * The decoded image read from the source input.
	 * 
	 * @return the <tt>BufferedImage</tt> representing the BMP image.
	 */
	public Image getImage() {
		return img;
	}

	private static void getColorTable(ColorEntry[] colorTable, byte[] ar, byte[] ag, byte[] ab) {
		for (int i = 0; i < colorTable.length; i++) {
			ar[i] = (byte) colorTable[i].bRed;
			ag[i] = (byte) colorTable[i].bGreen;
			ab[i] = (byte) colorTable[i].bBlue;
		}
	}

	/**
	 * Reads the BMP info header structure from the given <tt>InputStream</tt>.
	 * 
	 * @param lis the <tt>InputStream</tt> to read
	 * @return the <tt>InfoHeader</tt> structure
	 * @throws java.io.IOException if an error occurred
	 */
	public static InfoHeader readInfoHeader(LittleEndianInputStream lis) throws IOException {
		InfoHeader infoHeader = new InfoHeader(lis);
		return infoHeader;
	}

	/**
	 * @since 0.6
	 */
	public static InfoHeader readInfoHeader(LittleEndianInputStream lis, int infoSize)
			throws IOException {
		InfoHeader infoHeader = new InfoHeader(lis, infoSize);
		return infoHeader;
	}

	/**
	 * Reads the BMP data from the given <tt>InputStream</tt> using the information
	 * contained in the <tt>InfoHeader</tt>.
	 * 
	 * @param lis        the source input
	 * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}.
	 * @return the decoded image read from the source input
	 * @throws java.io.IOException if an error occurs
	 */
	public static Image read(InfoHeader infoHeader, LittleEndianInputStream lis)
			throws IOException {
		/* Color table (palette) */

		ColorEntry[] colorTable = null;

		// color table is only present for 1, 4 or 8 bit (indexed) images
		if (infoHeader.sBitCount <= 8) {
			colorTable = readColorTable(infoHeader, lis);
		}

		return read(infoHeader, lis, colorTable);
	}

	/**
	 * Reads the BMP data from the given <tt>InputStream</tt> using the information
	 * contained in the <tt>InfoHeader</tt>.
	 * 
	 * @param colorTable <tt>ColorEntry</tt> array containing palette
	 * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}.
	 * @param lis        the source input
	 * @return the decoded image read from the source input
	 * @throws java.io.IOException if any error occurs
	 */
	public static Image read(InfoHeader infoHeader, LittleEndianInputStream lis,
			ColorEntry[] colorTable) throws IOException {

		// 1-bit (monochrome) uncompressed
		if (infoHeader.sBitCount == 1 && infoHeader.iCompression == BMPConstants.BI_RGB) {

			return read1(infoHeader, lis, colorTable);

		}
		// 4-bit uncompressed
		else if (infoHeader.sBitCount == 4 && infoHeader.iCompression == BMPConstants.BI_RGB) {

			return read4(infoHeader, lis, colorTable);

		}
		// 8-bit uncompressed
		else if (infoHeader.sBitCount == 8 && infoHeader.iCompression == BMPConstants.BI_RGB) {

			return read8(infoHeader, lis, colorTable);

		}
		// 24-bit uncompressed
		else if (infoHeader.sBitCount == 24 && infoHeader.iCompression == BMPConstants.BI_RGB) {

			return read24(infoHeader, lis);

		}
		// 32bit uncompressed
		else if (infoHeader.sBitCount == 32 && infoHeader.iCompression == BMPConstants.BI_RGB) {

			return read32(infoHeader, lis);

		} else {
			throw new IOException("Unrecognized bitmap format: bit count=" + infoHeader.sBitCount + ", compression=" +
					infoHeader.iCompression);
		}

	}

	/**
	 * Reads the <tt>ColorEntry</tt> table from the given <tt>InputStream</tt> using
	 * the information contained in the given <tt>infoHeader</tt>.
	 * 
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @param lis        the <tt>InputStream</tt> to read
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static ColorEntry[] readColorTable(InfoHeader infoHeader, LittleEndianInputStream lis)
			throws IOException {
		ColorEntry[] colorTable = new ColorEntry[infoHeader.iNumColors];
		for (int i = 0; i < infoHeader.iNumColors; i++) {
			ColorEntry ce = new ColorEntry(lis);
			colorTable[i] = ce;
		}
		return colorTable;
	}

	/**
	 * Reads 1-bit uncompressed bitmap raster data, which may be monochrome
	 * depending on the palette entries in <tt>colorTable</tt>.
	 * 
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @param lis        the source input
	 * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
	 *                   must not be <tt>null</tt>.
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static Image read1(InfoHeader infoHeader,
			LittleEndianInputStream lis,
			ColorEntry[] colorTable) throws IOException {
		// 1 bit per pixel or 8 pixels per byte
		// each pixel specifies the palette index

		// Create indexed image
		WritableImage wimg = new WritableImage(infoHeader.iWidth, infoHeader.iHeight);
		PixelWriter pixelWriter = wimg.getPixelWriter();

		// padding

		int dataBitsPerLine = infoHeader.iWidth;
		int bitsPerLine = dataBitsPerLine;
		if (bitsPerLine % 32 != 0) {
			bitsPerLine = (bitsPerLine / 32 + 1) * 32;
		}
		int padBits = bitsPerLine - dataBitsPerLine;
		int padBytes = padBits / 8;

		int bytesPerLine = (int) (bitsPerLine / 8);
		int[] line = new int[bytesPerLine];

		for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
			for (int i = 0; i < bytesPerLine; i++) {
				line[i] = lis.readUnsignedByte();
			}

			for (int x = 0; x < infoHeader.iWidth; x++) {
				int i = x / 8; // номер байта, в котором цвет текущей точки по x
				int b = x % 8; // номер бита в байте с индексом цвета
				int v = line[i]; // сам байт с интексом цвета
				int index = getBit(v, b);
				ColorEntry ce = colorTable[index];

				pixelWriter.setColor(x, y, Color.rgb(ce.bRed, ce.bGreen, ce.bBlue));
			}
		}

		return wimg;
	}

	/**
	 * Reads 4-bit uncompressed bitmap raster data, which is interpreted based on
	 * the colours specified in the palette.
	 * 
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @param lis        the source input
	 * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
	 *                   must not be <tt>null</tt>.
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static Image read4(InfoHeader infoHeader,
			LittleEndianInputStream lis,
			ColorEntry[] colorTable) throws IOException {

		// 2 pixels per byte or 4 bits per pixel.
		// Colour for each pixel specified by the color index in the pallette.

		WritableImage wimg = new WritableImage(infoHeader.iWidth, infoHeader.iHeight);
		PixelWriter pixelWriter = wimg.getPixelWriter();

		// padding
		int bitsPerLine = infoHeader.iWidth * 4;
		if (bitsPerLine % 32 != 0) {
			bitsPerLine = (bitsPerLine / 32 + 1) * 32;
		}
		int bytesPerLine = (int) (bitsPerLine / 8);

		int[] line = new int[bytesPerLine];

		for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
			// scan line
			for (int i = 0; i < bytesPerLine; i++) {
				int b = lis.readUnsignedByte();
				line[i] = b;
			}

			// get pixels
			for (int x = 0; x < infoHeader.iWidth; x++) {
				// get byte index for line
				int b = x / 2; // 2 pixels per byte
				int i = x % 2;
				int n = line[b];
				int index = getNibble(n, i);
				ColorEntry ce = colorTable[index];

				pixelWriter.setColor(x, y, Color.rgb(ce.bRed, ce.bGreen, ce.bBlue));
			}
		}

		return wimg;
	}

	/**
	 * Reads 8-bit uncompressed bitmap raster data, which is interpreted based on
	 * the colours specified in the palette.
	 * 
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @param lis        the source input
	 * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
	 *                   must not be <tt>null</tt>.
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static Image read8(InfoHeader infoHeader,
			LittleEndianInputStream lis,
			ColorEntry[] colorTable) throws IOException {
		// 1 byte per pixel
		// color index 1 (index of color in palette)
		// lines padded to nearest 32bits
		// no alpha

		WritableImage wimg = new WritableImage(infoHeader.iWidth, infoHeader.iHeight);
		PixelWriter pixelWriter = wimg.getPixelWriter();

		// padding
		int dataPerLine = infoHeader.iWidth;
		int bytesPerLine = dataPerLine;
		if (bytesPerLine % 4 != 0) {
			bytesPerLine = (bytesPerLine / 4 + 1) * 4;
		}
		int padBytesPerLine = bytesPerLine - dataPerLine;

		for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
			for (int x = 0; x < infoHeader.iWidth; x++) {
				int b = lis.readUnsignedByte();
				ColorEntry ce = colorTable[b];
				pixelWriter.setColor(x, y, Color.rgb(ce.bRed, ce.bGreen, ce.bBlue));
			}

			lis.skipBytes(padBytesPerLine);
		}

		return wimg;
	}

	/**
	 * Reads 24-bit uncompressed bitmap raster data.
	 * 
	 * @param lis        the source input
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static Image read24(InfoHeader infoHeader,
			LittleEndianInputStream lis) throws IOException {
		// 3 bytes per pixel
		// blue 1
		// green 1
		// red 1
		// lines padded to nearest 32 bits
		// no alpha

		WritableImage wimg = new WritableImage(infoHeader.iWidth, infoHeader.iHeight);
		PixelWriter pixelWriter = wimg.getPixelWriter();

		// padding to nearest 32 bits
		int dataPerLine = infoHeader.iWidth * 3;
		int bytesPerLine = dataPerLine;
		if (bytesPerLine % 4 != 0) {
			bytesPerLine = (bytesPerLine / 4 + 1) * 4;
		}
		int padBytesPerLine = bytesPerLine - dataPerLine;

		for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
			for (int x = 0; x < infoHeader.iWidth; x++) {
				int b = lis.readUnsignedByte();
				int g = lis.readUnsignedByte();
				int r = lis.readUnsignedByte();

				pixelWriter.setColor(x, y, Color.rgb(r, g, b));
			}
			lis.skipBytes(padBytesPerLine);
		}

		return wimg;
	}

	/**
	 * Reads 32-bit uncompressed bitmap raster data, with transparency.
	 * 
	 * @param lis        the source input
	 * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
	 *                   {@link #readInfoHeader(LittleEndianInputStream)
	 *                   readInfoHeader()}
	 * @throws java.io.IOException if an error occurs
	 * @return the decoded image read from the source input
	 */
	public static Image read32(InfoHeader infoHeader,
			LittleEndianInputStream lis) throws IOException {
		// 4 bytes per pixel
		// blue 1
		// green 1
		// red 1
		// alpha 1
		// No padding since each pixel = 32 bits

		WritableImage wimg = new WritableImage(infoHeader.iWidth, infoHeader.iHeight);
		PixelWriter pixelWriter = wimg.getPixelWriter();

		for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
			for (int x = 0; x < infoHeader.iWidth; x++) {
				int b = lis.readUnsignedByte();
				int g = lis.readUnsignedByte();
				int r = lis.readUnsignedByte();
				int a = lis.readUnsignedByte();

				pixelWriter.setColor(x, y, Color.rgb(r, g, b, a / 255d));
			}
		}

		return wimg;
	}

}
