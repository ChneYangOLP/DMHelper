import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Init_DB;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    private static final String APP_ICON_RESOURCE = "/com/DMHelper/assets/app_icon.png";
    private static Image cachedAppIcon;
    private static boolean coreBootstrapped;

    public static void main(String[] args) {
        configure_platform_settings();
        install_global_exception_handler();

        try_launch_javafx(args);
    }

    static synchronized void ensure_core_bootstrapped() {
        if (coreBootstrapped) {
            return;
        }
        Init_DB.setup_database();
        Custom_Equipment_DAO.load_all_custom_items();
        Character_DAO.load_all_characters();
        apply_application_icon();
        coreBootstrapped = true;
    }

    private static void configure_platform_settings() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        System.setProperty("java.awt.headless", "false");

        if (osName.contains("mac")) {
            System.setProperty("apple.awt.application.name", "DMD Helper");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.appearance", "system");
        }
    }

    private static void install_global_exception_handler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> show_startup_error(throwable));
    }

    private static void apply_application_icon() {
        Image icon = load_app_icon();
        if (icon == null) {
            return;
        }

        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().setIconImage(icon);
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
    }

    private static Image load_app_icon() {
        if (cachedAppIcon != null) {
            return cachedAppIcon;
        }

        try (InputStream inputStream = Main.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }
            cachedAppIcon = ImageIO.read(inputStream);
            return cachedAppIcon;
        } catch (IOException ignored) {
            return null;
        }
    }

    static String getAppIconResourcePath() {
        return APP_ICON_RESOURCE;
    }

    private static void show_startup_error(Throwable throwable) {
        throwable.printStackTrace();

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        String message = "程序启动失败：\n"
                + throwable.getClass().getSimpleName()
                + (throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()
                ? ""
                : "\n" + throwable.getMessage());

        JOptionPane.showMessageDialog(
                null,
                message,
                "DMD Helper 启动错误",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static void try_launch_javafx(String[] args) {
        try {
            FxLauncher.launchApp(args);
        } catch (Throwable throwable) {
            show_startup_error(throwable);
        }
    }
}
