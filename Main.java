import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ecs100.UI;
import ecs100.UIFileChooser;

public class Main {

	BufferedImage image;

	private Main() {
		UI.initialise();
		UI.addButton("Load", this::load);
		UI.addButton("Convert", this::convert);
	}

	private void load() {
		String file = UIFileChooser.open();
		try {
			image = ImageIO.read(new File(file));
		} catch (IOException e) {
			e.printStackTrace();
		}

		UI.drawImage(image, 0, 0);
	}

	private void convert() {
		for(int x = 0; x < image.getWidth(); x++) {
			for(int y = 0; y < image.getHeight(); y++) {
				Color c = new Color(image.getRGB(x, y));
				int i = c.

			}
		}
	}

	public static void main(String[] args) {
		new Main();
	}

}
