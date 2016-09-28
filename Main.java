package ToWebSite;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/* Code for Assignment ??
 * Name:
 * Usercode:
 * ID:
 */
import ecs100.UI;
import ecs100.UIFileChooser;


/** <description of class Main>
 */
public class Main{

    private Arm arm;
    private Drawing drawing;
    private ToolPath tool_path;
    // state of the GUI
    private int state; // 0 - nothing
                       // 1 - inverse point kinematics - point
                       // 2 - enter path. Each click adds point
                       // 3 - enter path pause. Click does not add the point to the path

    /**      */
    public Main(){
        UI.initialise();
        UI.addButton("Clear", () -> {drawing = new Drawing();});
        UI.addButton("xy to angles", this::inverse);
        UI.addButton("Enter path XY", this::enter_path_xy);
        UI.addButton("Save path XY", this::save_xy);
        UI.addButton("Load path XY", this::load_xy);
        UI.addButton("Save path Ang", this::save_ang);
        UI.addButton("Load path Ang:Play", this::load_ang);
        UI.addButton("Excecute", this::excecute);
        UI.addButton("Draw Circle", this::circle);
        UI.addButton("Draw Square", this::square);
        UI.addButton("Draw Pic", this::pic);

       // UI.addButton("Quit", UI::quit);
        UI.setMouseMotionListener(this::doMouse);
        UI.setKeyListener(this::doKeys);


        /*try {
			ServerSocket serverSocket = new ServerSocket(22);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
        tool_path = new ToolPath();
        this.arm = new Arm();
        this.drawing = new Drawing();
        this.run();
        arm.draw();
    }

    public void doKeys(String action){
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
    	for(int i = 0; i <= 380; i+=20) {
    		drawing.add_point_to_path(x+r*Math.cos(Math.toRadians(i)), y+r*Math.sin(Math.toRadians(i)), true);
    	}
    }

    public void square() {
    	drawing.add_point_to_path(330, 125, true);
    	drawing.add_point_to_path(330+50, 125, true);
    	drawing.add_point_to_path(330+50, 125+50, true);
    	drawing.add_point_to_path(330, 125+50, true);
    	drawing.add_point_to_path(330, 125, true);
    }

    public void pic() {
    	String file = UIFileChooser.open();
    	if(file == null) return;
    	try {
			BufferedImage bi = ImageIO.read(new File(file));
			UI.drawImage(bi, 0, 0);
			bi = edge(bi);
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

    public BufferedImage edge(BufferedImage in) {
    	BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), in.getType());
    	for(int x = 1; x < in.getWidth()-1; x++) {
    		for(int y = 1; y < in.getHeight()-1; y++) {
    			int tot = 0;
    			for(int i = x-1; i <= x+1; i++) {
    				for(int j = y-1; j <= y+1; j++) {
    					int R = 255-new Color(in.getRGB(i, j)).getRed();
    					int G = 255-new Color(in.getRGB(i, j)).getGreen();
    					int B = 255-new Color(in.getRGB(i, j)).getBlue();
    					tot += (x==i&&y==j?8:-1)*(R+G+B)/3;
    				}
    			}
    			out.setRGB(x, y, new Color(tot).getRGB());
    		}
    	}
    	UI.drawImage(out, 0, 0);
    	return out;
    }

    public void doMouse(String action, double x, double y) {
         //UI.printf("Mouse Click:%s, state:%d  x:%3.1f  y:%3.1f\n",
         //   action,state,x,y);
        UI.clearGraphics();
        String out_str=String.format("%3.1f %3.1f",x,y);
        UI.drawString(out_str, x+10,y+10);
         //
         if ((state == 1)&&(action.equals("clicked"))){
          // draw as

          arm.inverseKinematic(x,y);
          //arm.draw();
          return;
        }

         if ( ((state == 2)||(state == 3))&&action.equals("moved") ){
          // draw arm and path
          arm.inverseKinematic(x,y);
          //arm.draw();

          // draw segment from last entered point to current mouse position
          if ((state == 2)&&(drawing.get_path_size()>0)){
            PointXY lp = new PointXY();
            lp = drawing.get_path_last_point();
            //if (lp.get_pen()){
               UI.setColor(Color.GRAY);
               UI.drawLine(lp.get_x(),lp.get_y(),x,y);
           // }
          }
           drawing.draw();
        }

        // add point
        if (   (state == 2) &&(action.equals("clicked"))){
            // add point(pen down) and draw
            UI.printf("Adding point x=%f y=%f\n",x,y);
            if(arm.isValid()) drawing.add_point_to_path(x,y,true); // add point with pen down

            arm.inverseKinematic(x,y);
            //arm.draw();
            drawing.draw();
            drawing.print_path();
        }


        if (   (state == 3) &&(action.equals("clicked"))){
            // add point and draw
            //UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x,y,false); // add point wit pen up

            arm.inverseKinematic(x,y);
            //arm.draw();
            drawing.draw();
            drawing.print_path();
            state = 2;
        }


    }


    public void save_xy(){
        state = 0;
        String fname = UIFileChooser.save();
        drawing.save_path(fname);
    }

    public void enter_path_xy(){
         state = 2;
    }

    public void inverse(){
         state = 1;
         //arm.draw();
    }

    public void load_xy(){
        state = 0;
        String fname = UIFileChooser.open();
        drawing.load_path(fname);
        drawing.draw();

        //arm.draw();
    }

    // save angles into the file
    public void save_ang(){
        String fname = UIFileChooser.open();
        tool_path.convert_drawing_to_angles(drawing,arm,fname);
        tool_path.save_angles(fname);
    }


    public void load_ang(){

    }

    public void run() {
        while(true) {
            arm.draw();
            drawing.draw();
            UI.sleep(20);
        }
    }

    private void excecute() {
    	/*try {
			Runtime.getRuntime().exec("scp file.ext username@host:/remote/directory");
		} catch (IOException e) {
			e.printStackTrace();
		}*/
    	tool_path.convert_drawing_to_angles(drawing, arm, "");
    	tool_path.convert_angles_to_pwm(arm);
    	tool_path.save_PWM(UIFileChooser.open());
    }

    public static void main(String[] args){
        new Main();
    }

}
