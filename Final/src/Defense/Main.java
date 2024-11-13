package Defense;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
       try {
    	Parent root = FXMLLoader.load(getClass().getResource("Defense.fxml"));
        primaryStage.setTitle("Defense");
        primaryStage.setScene(new Scene(root,1168, 664));
        primaryStage.show();
       } catch (Exception e) {
    	e.printStackTrace();
       }
    }
    public static void main(String[] args) {
        launch(args);
    }
}