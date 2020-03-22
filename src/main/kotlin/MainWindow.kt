import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class MainWindow : Application() {

    override fun start(primaryStage: Stage) {

        val scene = Scene(FXMLLoader.load<Parent>(this.javaClass.getResource("fxml/app.fxml")))
        scene.stylesheets.addAll(this::class.java.getResource("ChartLine.css").toExternalForm())
        primaryStage.scene = scene

        val controller = AppController()


        primaryStage.show()
    }

}