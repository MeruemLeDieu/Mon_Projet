
import dagger.BindsInstance
import dagger.Component
import fr.william.camera_app.CameraAppApplication
import javax.inject.Singleton


@Singleton
@Component
interface MyApplicationComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: CameraAppApplication): MyApplicationComponent
    }
}