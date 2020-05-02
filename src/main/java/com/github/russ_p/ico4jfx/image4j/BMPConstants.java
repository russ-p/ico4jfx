package com.github.russ_p.ico4jfx.image4j;

/**
 * Provides constants used with BMP format.
 * @author Ian McDonagh
 */
public class BMPConstants {
  
  private BMPConstants() { }
  
  /**
   * The signature for the BMP format header "BM".
   */
  public static final String FILE_HEADER = "BM";
  
  /**
   * Specifies no compression.
   * @see InfoHeader#iCompression InfoHeader
   */
  public static final int BI_RGB = 0; //no compression
  /**
   * Specifies 8-bit RLE compression.
   * @see InfoHeader#iCompression InfoHeader
   */
  public static final int BI_RLE8 = 1; //8bit RLE compression
  /**
   * Specifies 4-bit RLE compression.
   * @see InfoHeader#iCompression InfoHeader
   */
  public static final int BI_RLE4 = 2; //4bit RLE compression  
}
