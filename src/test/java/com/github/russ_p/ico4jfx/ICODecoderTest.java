package com.github.russ_p.ico4jfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.scene.image.Image;

public class ICODecoderTest {

	@Test
	public void testReadHabr() throws Exception {
		List<Image> images = ICODecoder.read(get("https://habr.com/favicon.ico"));
		assertFalse(images.isEmpty());
		
		double minH = images.stream().mapToDouble(Image::getHeight).min().getAsDouble();
		double maxH = images.stream().mapToDouble(Image::getHeight).max().getAsDouble();
		
		assertEquals(16.0, minH);
		assertEquals(48.0, maxH);
	}

	@Test
	public void testReadLOR() throws Exception {
		List<Image> images = ICODecoder.read(get("https://www.linux.org.ru/favicon.ico"));
		assertFalse(images.isEmpty());
		
		double minH = images.stream().mapToDouble(Image::getHeight).min().getAsDouble();
		double maxH = images.stream().mapToDouble(Image::getHeight).max().getAsDouble();
		
		assertEquals(16.0, minH);
		assertEquals(32.0, maxH);
	}

	@Test
	public void testReadEaxMe() throws Exception {
		List<Image> images = ICODecoder.read(get("https://eax.me/favicon.ico"));
		assertFalse(images.isEmpty());
		
		double minH = images.stream().mapToDouble(Image::getHeight).min().getAsDouble();
		double maxH = images.stream().mapToDouble(Image::getHeight).max().getAsDouble();
		
		assertEquals(16.0, minH);
		assertEquals(16.0, maxH);
	}

	@Test
	public void testReadHackerNews() throws Exception {
		List<Image> images = ICODecoder.read(get("https://news.ycombinator.com/favicon.ico"));
		assertFalse(images.isEmpty());
		
		double minH = images.stream().mapToDouble(Image::getHeight).min().getAsDouble();
		double maxH = images.stream().mapToDouble(Image::getHeight).max().getAsDouble();
		
		assertEquals(256.0, minH);
		assertEquals(256.0, maxH);
	}

	private ByteArrayInputStream get(String url) throws MalformedURLException, IOException {
		try (InputStream istream = new URL(url).openStream();) {

			ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			while ((bytesRead = istream.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
			return new ByteArrayInputStream(out.toByteArray());
		}
	}

}
