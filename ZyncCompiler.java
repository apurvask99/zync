import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ZyncCompiler implements ActionListener {
  
  private JLabel label = new JLabel("Location of Zync file: No file selected");
  private JFrame frame = new JFrame();
  private String location;
  private static ArrayList<String> imports = new ArrayList<String>(); 
  
  public ZyncCompiler() {
    // the compile button
    JButton button = new JButton("Compile");
    button.addActionListener(this);
    
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
    int result = fileChooser.showOpenDialog(fileChooser);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      label.setText("Location of Zync file: " + selectedFile.getAbsolutePath());
      location = selectedFile.getAbsolutePath().toString();
    }
    
    // the panel with the button and text
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 10, 50));
    panel.setLayout(new GridLayout(0, 1));
    panel.add(label);
    panel.add(button);
    
    // set up the frame and display it
    frame.add(panel, BorderLayout.CENTER);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("Zync Compiler v1.4.10");
    frame.pack();
    frame.setVisible(true);
  }
  
  // process the button click
  public void actionPerformed(ActionEvent e) {
    try {
      if (location == null){
        JOptionPane.showMessageDialog(frame, "No file imported. Please restart the program and try again.");
      }
      // validate that it's a .zync file -- this might cause some problems and we should revise this
      if (!location.substring(location.length() - 5).equalsIgnoreCase(".zync")){
        JOptionPane.showMessageDialog(frame, "Error: The file you entered does not seem to be a Zync file. Please exit the program and try again.");
      }
      // load the source file into an array
      BufferedReader br = new BufferedReader(new FileReader(location));
      String line;
      ArrayList<String> body = new ArrayList<String>(); // we treat the file as an array of Strings for convenience
      while ((line = br.readLine()) != null){
        body.add(line);
      }
      br.close();
      // process and modify the file inside the array
      ArrayList<String> finalBody = new ArrayList<String>();
      
      Pattern mainMethod = Pattern.compile(".*?main(\\s*)\\{");
      Pattern doFeature = Pattern.compile(".*?do\\s*(.*?)");
      Pattern random = Pattern.compile(".*?random\\s*(.*?)");
      Pattern ask = Pattern.compile(".*?ask\\s*\\((int|String|double)\\).*?");
      Pattern prompt = Pattern.compile(".*?(prompt|showMessage)\\s*\\(.*?\\).*?");
      Pattern toInt = Pattern.compile(".*?toInt\\s*\\(.*?\\).*?");
      boolean scannerImported = false, randomImported = false, JOPImported = false;
      
      for (String i: body){
        i = i.replace("print", "System.out.print");
        Matcher main = mainMethod.matcher(i);
        Matcher m = doFeature.matcher(i);
        Matcher r = random.matcher(i);
        Matcher a = ask.matcher(i);
        Matcher p = prompt.matcher(i);
        Matcher t = toInt.matcher(i);
        
        if (main.matches()){
          try {
            i = i.replaceAll("main", "public static void main (String [] args) throws Exception");
          } catch (Exception q) {
            q.printStackTrace();
            JOptionPane.showMessageDialog(frame, "There was an error processing the main method declaration. Please check your code and try again.");
          }
        }
        if (m.matches()){
          try {
            int temp = Integer.parseInt(m.group(1).substring(1, m.group(1).indexOf(")")));
            //System.out.println(m.group(1).substring(1, m.group(1).indexOf(")"))); // DEBUG
            i = i.replaceAll("do\\s*(.*?)", ("for (int i = 0; i < " + temp + "; i++) {"));
            i = i.substring(0, i.indexOf("{") + 1);
          } catch (Exception f){
            f.printStackTrace();
          }
        }
        if (r.matches()){
          try {
            int q = Integer.parseInt(r.group(1).substring(1, r.group(1).indexOf(")")));
            i = i.replaceAll("random\\s*(.*?)\\)", ("rand.nextInt(" + q + ")"));
            if (!randomImported){
              imports.add(0, "import java.util.Random;");
              randomImported = true;
            }
          } catch (Exception f){
            f.printStackTrace();
            JOptionPane.showMessageDialog(frame, "There was an error processing a random(x) call. Please check your code and try again.");
          }
        }
        if (a.matches()){ 
          if (!scannerImported){
            imports.add(0, "import java.util.Scanner;");
            scannerImported = true;
          }
        }
        if (p.matches()){ 
          if (!JOPImported){
            imports.add(0, "import javax.swing.JOptionPane;");
            JOPImported = true;
          }
          i = i.replaceAll("prompt", "JOptionPane.showInputDialog");
          i = i.replaceAll("showMessage\\s*\\(", "JOptionPane.showMessageDialog(null, ");
        }
        if (t.matches()){
          i = i.replaceAll("toInt", "Integer.parseInt");
        }
        i = i.replaceAll("ask\\s*\\(int\\)", "in.nextInt()");
        i = i.replaceAll("ask\\s*\\(String\\)", "in.nextLine()");
        i = i.replaceAll("ask\\s*\\(double\\)", "in.nextDouble()");
        finalBody.add(i);
      }
      // import everything into finalBody
      addImports(finalBody);
      // write finalBody into a file
      BufferedWriter bw = new BufferedWriter(new FileWriter(location.substring(0, location.length() - 5) + ".java"));
      for (String i: finalBody) {
        bw.write(i + System.lineSeparator());
        // declare any imported libraries
        if (scannerImported && i.indexOf("public static void main (String [] args) throws Exception") > 0){
          bw.write("\t\tScanner in = new Scanner (System.in);" + System.lineSeparator());
        }
        if (randomImported && i.indexOf("public static void main (String [] args) throws Exception") > 0){
          bw.write("\t\tRandom rand = new Random();" + System.lineSeparator());
        }
      }
      bw.close();
      JOptionPane.showMessageDialog(frame, "Success.");
    } catch (Exception f){
      f.printStackTrace();
      JOptionPane.showMessageDialog(frame, "An unexpected error occurred. Please try again.");
    }
  }
  public static void addImports(ArrayList<String> finalBody){
    if (imports.size() != 0){
      finalBody.add(0, "");
      for (String i:imports){
        finalBody.add(0, i);
      }
    }
  }
  // create a GUI frame
  public static void main(String[] args) {
    new ZyncCompiler();
  }
}
