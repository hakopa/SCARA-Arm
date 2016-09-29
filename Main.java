package ToWebSite;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.imageio.ImageIO;

/* Code for Assignment ??
 * Name:
 * Usercode:
 * ID:
 */
import ecs100.UI;
import ecs100.UIFileChooser;

/**
 * <description of class Main>
 */
public class Main {

	private Arm arm;
	private Drawing drawing;
	private ToolPath tool_path;
	// state of the GUI
	private int state; // 0 - nothing
						// 1 - inverse point kinematics - point
						// 2 - enter path. Each click adds point
						// 3 - enter path pause. Click does not add the point to
						// the path

	/**      */
	public Main() {
		UI.initialise();
		UI.addButton("Clear", () -> {
			drawing = new Drawing();
			tool_path = new ToolPath();
			drawing.draw();
		});
		UI.addButton("xy to angles", this::inverse);
		UI.addButton("Enter path XY", this::enter_path_xy);
		UI.addButton("Save path XY", this::save_xy);
		UI.addButton("Load path XY", this::load_xy);
		UI.addButton("Save path Ang", this::save_ang);
		UI.addButton("Load path Ang:Play", this::load_ang);
		UI.addButton("Excecute", this::excecute);
		UI.addButton("Draw Circle", this::circle);
		UI.addButton("Draw Square", this::square);
		UI.addButton("Draw Image", this::pic);
		UI.addButton("Draw Image 2", this::pic2);

		// UI.addButton("Quit", UI::quit);
		UI.setMouseMotionListener(this::doMouse);
		UI.setKeyListener(this::doKeys);

		/*
		 * try { ServerSocket serverSocket = new ServerSocket(22); } catch
		 * (IOException e) { e.printStackTrace(); }
		 */
		tool_path = new ToolPath();
		this.arm = new Arm();
		this.drawing = new Drawing();
		this.run();
		arm.draw();
	}

	public void doKeys(String action) {
		UI.printf("Key :%s \n", action);
		if (action.equals("b")) {
			// break - stop entering the lines
			state = 3;
			//

		}

	}

	public void circle() {
		int x = 330;
		int y = 125;
		int r = 20;
		for (int i = 0; i <= 380; i += 20) {
			drawing.add_point_to_path(x + r * Math.cos(Math.toRadians(i)), y + r * Math.sin(Math.toRadians(i)), true);
		}
	}

	public void square() {
		drawing.add_point_to_path(330, 125, true);
		drawing.add_point_to_path(330 + 50, 125, true);
		drawing.add_point_to_path(330 + 50, 125 + 50, true);
		drawing.add_point_to_path(330, 125 + 50, true);
		drawing.add_point_to_path(330, 125, true);
	}

	public void pic2() {
		UI.clearGraphics();
		String file = UIFileChooser.open();
		if(file == null) return;
		try {
			BufferedImage bi = ImageIO.read(new File(file));

			double xOrigin = 250;
			double yOrigin = 110;
			double dx = 450;
			double dy = 180;
			double s1 = (dx-xOrigin)/bi.getWidth();
			double s2 = (dy-yOrigin)/bi.getHeight();
			double scale = s1<s2 ? s1 : s2;

			UI.drawImage(bi, 0, 0);
			BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
			for(int y = 0; y < bi.getHeight(); y++) {
				boolean penDown = false;
				for(int x = 0; x < bi.getWidth(); x++) {
					if(y%2 == 1) {
						out.setRGB(x, y, 0xFFFFFF);
						continue;
					}
					if(shouldPenBeDown(bi, x, y) != penDown) {
						penDown = !penDown;
						drawing.add_point_to_path(xOrigin+x*scale, yOrigin+y*scale, !penDown);
					}
					out.setRGB(x, y, penDown ? 0x0 : 0xFFFFFF);
				}
			}
			UI.clearGraphics();
			UI.drawImage(out, 0, 0);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean shouldPenBeDown(BufferedImage in, int x, int y) {
		Color c = new Color(in.getRGB(x, y));
		return (c.getRed()+c.getGreen()+c.getBlue())/3 < 128;
	}

	public void pic() {
		UI.clearGraphics();
		String file = UIFileChooser.open();
		if (file == null)
			return;
		try {
			BufferedImage bi = ImageIO.read(new File(file));
			UI.drawImage(bi, 0, 0);
			//UI.clearGraphics();
			bi = resize(bi);
			bi = edge(bi);
			//bi = filterDots(bi);
			UI.drawImage(bi, 0, 0);
			convertToLines(bi);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public BufferedImage resize(BufferedImage in) {
		double scale = 200d/in.getWidth() < 200d/in.getHeight() ? 200d/in.getWidth() : 200d/in.getHeight();
		BufferedImage out = new BufferedImage((int)(in.getWidth()*scale), (int)(in.getHeight()*scale), in.getType());
		for(int x = 0; x < out.getWidth(); x++) {
			for(int y = 0; y < out.getHeight(); y++) {
				int R = 0;
				int G = 0;
				int B = 0;
				int i = 0;
				int j = 0;
				for(i = (int)(x/scale); i < x+(1/scale); i++) {
					for(j = (int)(y/scale); j < y+(1/scale); j++) {
						R += (in.getRGB(i, j) & 0xFF0000) >> 32;
						G += (in.getRGB(i, j) & 0xFF00) >> 16;
						B += (in.getRGB(i, j) & 0xFF);
					}
				}
				if(i == 0 || j == 0) return in;
				R /= i*j;
				G /= i*j;
				B /= i*j;
				int rgb = (R << 32)+(G << 16)+B;
				out.setRGB(x, y, rgb);
			}
		}
		return out;
	}

	public BufferedImage edge(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth() - 2, in.getHeight() - 2, in.getType());
		for (int x = 1; x < in.getWidth() - 1; x++) {
			for (int y = 1; y < in.getHeight() - 1; y++) {
				int tot = 0;
				for (int i = x - 1; i <= x + 1; i++) {
					for (int j = y - 1; j <= y + 1; j++) {
						int R = new Color(in.getRGB(i, j)).getRed();
						int G = new Color(in.getRGB(i, j)).getGreen();
						int B = new Color(in.getRGB(i, j)).getBlue();
						tot += (x == i && y == j ? 8 : -1) * (R + G + B) / 3;
					}
				}
				tot = tot > 50 ? 0 : 0xFFFFFF;
				out.setRGB(x - 1, y - 1, new Color(tot).getRGB());
			}
		}
		UI.drawImage(out, 0, 0);
		return out;
	}

	public BufferedImage filterDots(BufferedImage in) {
		BufferedImage out = copy(in);
		for (int x = 0; x < out.getWidth(); x++) {
			for (int y = 0; y < out.getHeight(); y++) {
				int count = countAdjacentPixels(out, x, y);
				if (count < 5 && count > 0) {
					removeLine(out, x, y);
				}
			}
		}
		UI.drawImage(out, 0, 0);
		return out;
	}

	public BufferedImage copy(BufferedImage in) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), in.getType());
		for (int x = 0; x < in.getWidth(); x++) {
			for (int y = 0; y < in.getHeight(); y++) {
				out.setRGB(x, y, in.getRGB(x, y));
			}
		}
		return out;
	}

	public int countAdjacentPixels(BufferedImage in, int x, int y) {
		Set<PointXY> visited = new HashSet<PointXY>();
		Stack<PointXY> todo = new Stack<PointXY>();
		if ((in.getRGB(x, y) & 0xFF) > 128)
			return 0;
		todo.push(new PointXY(x, y, true));
		int count = 0;
		while (!todo.isEmpty()) {
			count++;
			PointXY p = todo.pop();
			x = (int) p.get_x();
			y = (int) p.get_y();
			if (x + 1 < in.getWidth() && !contains(visited, new PointXY(x + 1, y, true))
					&& (in.getRGB(x + 1, y) & 0xFF) < 128)
				todo.push(new PointXY(x + 1, y, true));
			if (x - 1 >= 0 && !contains(visited, new PointXY(x - 1, y, true)) && (in.getRGB(x - 1, y) & 0xFF) < 128)
				todo.push(new PointXY(x - 1, y, true));
			if (y + 1 < in.getHeight() && !contains(visited, new PointXY(x, y + 1, true))
					&& (in.getRGB(x, y + 1) & 0xFF) < 128)
				todo.push(new PointXY(x, y + 1, true));
			if (y - 1 >= 0 && !contains(visited, new PointXY(x, y - 1, true)) && (in.getRGB(x, y - 1) & 0xFF) < 128)
				todo.push(new PointXY(x, y - 1, true));
			visited.add(p);
		}

		return count;// AdjacentPixels(in, x, y, null);
	}

	@SuppressWarnings("rawtypes")
	public boolean contains(Set set, Object o) {
		for(Object s : set) {
			if(s.equals(o)) return true;
		}
		return false;
	}

	public int countAdjacentPixels(BufferedImage in, int x, int y, Set<PointXY> points) {
		if (points == null)
			points = new HashSet<PointXY>();
		// UI.println(in.getRGB(x, y) & 0b11111111);
		if (points.contains(new PointXY(x, y, true)) || (in.getRGB(x, y) & 0xFF) < 128)
			return 0;
		points.add(new PointXY(x, y, true));
		int tot = 1;
		tot += x + 1 < in.getWidth() ? countAdjacentPixels(in, x + 1, y, points) : 0;
		tot += x - 1 >= 0 ? countAdjacentPixels(in, x - 1, y, points) : 0;
		tot += y + 1 < in.getHeight() ? countAdjacentPixels(in, x, y + 1, points) : 0;
		tot += y - 1 >= 0 ? countAdjacentPixels(in, x, y - 1, points) : 0;
		return tot;
	}

	public void removeLine(BufferedImage in, int x, int y) {
		/*
		 * if((in.getRGB(x, y) & 0x00F0) < 128) return; in.setRGB(x, y, 0xFFFF);
		 * removeLine(in, x+1, y); removeLine(in, x-1, y); removeLine(in, x,
		 * y+1); removeLine(in, x, y-1);
		 */
		Stack<PointXY> todo = new Stack<PointXY>();
		todo.push(new PointXY(x, y, true));
		while (!todo.isEmpty()) {
			PointXY p = todo.pop();
			x = (int) p.get_x();
			y = (int) p.get_y();
			in.setRGB(x, y, 0xFFFFFFFF);
			if (x + 1 < in.getWidth() && (in.getRGB(x + 1, y) & 0xFF) < 128)
				todo.push(new PointXY(x + 1, y, true));
			if (x - 1 >= 0 && (in.getRGB(x - 1, y) & 0xFF) < 128)
				todo.push(new PointXY(x - 1, y, true));
			if (y + 1 < in.getHeight() && (in.getRGB(x, y + 1) & 0xFF) < 128)
				todo.push(new PointXY(x, y + 1, true));
			if (y - 1 >= 0 && (in.getRGB(x, y - 1) & 0xFF) < 128)
				todo.push(new PointXY(x, y - 1, true));
		}
	}
	public void convertToLines(BufferedImage in) {

		double xOrigin = 300;
		double yOrigin = 110;
		double dx = 450;
		double dy = 170;
		double s1 = (dx-xOrigin)/in.getWidth();
		double s2 = (dy-yOrigin)/in.getHeight();
		double scale = s1<s2 ? s1 : s2;

		Set<PointXY> visited = new HashSet<PointXY>();
		for(int x = 0; x < in.getWidth(); x++) {
			for(int y = 0; y < in.getHeight(); y++) {
				PointXY pixel = new PointXY(x, y, false);
				if((in.getRGB(x, y) & 0xFF) < 128 && !contains(visited, pixel)) {
					Stack<PointXY> todo = new Stack<PointXY>();
					todo.add(pixel);
					while(!todo.isEmpty()) {
						PointXY p = todo.pop();
						int i = (int)p.get_x();
						int j = (int)p.get_y();
							 if(checkValid(i+1, j+1, in, visited)) todo.push(new PointXY(i+1, j+1, true));
						else if(checkValid(i+1, j+0, in, visited)) todo.push(new PointXY(i+1, j+0, true));
						else if(checkValid(i+1, j-1, in, visited)) todo.push(new PointXY(i+1, j-1, true));
						else if(checkValid(i-1, j+1, in, visited)) todo.push(new PointXY(i-1, j+1, true));
						else if(checkValid(i-1, j+0, in, visited)) todo.push(new PointXY(i-1, j+0, true));
						else if(checkValid(i-1, j-1, in, visited)) todo.push(new PointXY(i-1, j-1, true));
						else if(checkValid(i+0, j+1, in, visited)) todo.push(new PointXY(i+0, j+1, true));
						else if(checkValid(i+0, j-1, in, visited)) todo.push(new PointXY(i+0, j-1, true));
						drawing.add_point_to_path(xOrigin+p.get_x()*scale, yOrigin+p.get_y()*scale, p.get_pen());
						visited.add(p);
					}
				}
			}
		}
	}

	public boolean checkValid(int x, int y, BufferedImage in, Set<PointXY> set) {
		return x >= 0 && x < in.getWidth() && y >= 0 && y < in.getHeight() && (in.getRGB(x, y) & 0xFF) < 128 && !contains(set, new PointXY(x, y, true));
	}


	public void doMouse(String action, double x, double y) {
		// UI.printf("Mouse Click:%s, state:%d x:%3.1f y:%3.1f\n",
		// action,state,x,y);
		UI.clearGraphics();
		String out_str = String.format("%3.1f %3.1f", x, y);
		UI.drawString(out_str, x + 10, y + 10);
		//
		if ((state == 1) && (action.equals("clicked"))) {
			// draw as

			arm.inverseKinematic(x, y);
			// arm.draw();
			return;
		}

		if (((state == 2) || (state == 3)) && action.equals("moved")) {
			// draw arm and path
			arm.inverseKinematic(x, y);
			// arm.draw();

			// draw segment from last entered point to current mouse position
			if ((state == 2) && (drawing.get_path_size() > 0)) {
				PointXY lp = new PointXY();
				lp = drawing.get_path_last_point();
				// if (lp.get_pen()){
				UI.setColor(Color.GRAY);
				UI.drawLine(lp.get_x(), lp.get_y(), x, y);
				// }
			}
			drawing.draw();
		}

		// add point
		if ((state == 2) && (action.equals("clicked"))) {
			// add point(pen down) and draw
			UI.printf("Adding point x=%f y=%f\n", x, y);
			if (arm.isValid())
				drawing.add_point_to_path(x, y, true); // add point with pen
														// down

			arm.inverseKinematic(x, y);
			// arm.draw();
			drawing.draw();
			drawing.print_path();
		}

		if ((state == 3) && (action.equals("clicked"))) {
			// add point and draw
			// UI.printf("Adding point x=%f y=%f\n",x,y);
			drawing.add_point_to_path(x, y, false); // add point wit pen up

			arm.inverseKinematic(x, y);
			// arm.draw();
			drawing.draw();
			drawing.print_path();
			state = 2;
		}

	}

	public void save_xy() {
		state = 0;
		String fname = UIFileChooser.save();
		drawing.save_path(fname);
	}

	public void enter_path_xy() {
		state = 2;
	}

	public void inverse() {
		state = 1;
		// arm.draw();
	}

	public void load_xy() {
		state = 0;
		String fname = UIFileChooser.open();
		drawing.load_path(fname);
		drawing.draw();

		// arm.draw();
	}

	// save angles into the file
	public void save_ang() {
		String fname = UIFileChooser.open();
		tool_path.convert_drawing_to_angles(drawing, arm, fname);
		tool_path.save_angles(fname);
	}

	public void load_ang() {

	}

	public void run() {
		while (true) {
			arm.draw();
			drawing.draw();
			UI.sleep(20);
		}
	}

	private void excecute() {
		tool_path.convert_drawing_to_angles(drawing, arm, "");
		tool_path.convert_angles_to_pwm(arm);
		tool_path.save_PWM(UIFileChooser.open());
	}

	public static void main(String[] args) {
		new Main();
	}

}
