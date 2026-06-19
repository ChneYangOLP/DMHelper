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
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import com.DMHelper.api.ApiServer;

public class Main {
    private static final String APP_ICON_RESOURCE = "/com/DMHelper/assets/app_icon.png";
    private static Image cachedAppIcon;
    private static boolean coreBootstrapped;

    public static void main(String[] args) {
        configure_platform_settings();
        install_global_exception_handler();
        
        ensure_core_bootstrapped();

        if (Arrays.asList(args).contains("--server")) {
            System.out.println("[Main] Starting in Server Mode...");
            ApiServer.start(8080);
        } else if (Arrays.asList(args).contains("--javafx")) {
            System.out.println("[Main] Starting Legacy JavaFX Mode...");
            try_launch_javafx(args);
        } else {
            System.out.println("[Main] Starting Local API Server and Launching Electron Frontend...");
            ApiServer.start(8080);
            
            try {
                // Use a ProcessBuilder to launch npm in the frontend directory
                File frontendDir = new File(System.getProperty("user.dir"), "frontend");
                ProcessBuilder pb = new ProcessBuilder("npm", "run", "electron:dev");
                // Fallback for macOS if npm is not in PATH
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    pb = new ProcessBuilder("/bin/bash", "-c", "source ~/.bash_profile 2>/dev/null || source ~/.zshrc 2>/dev/null || true; npm run electron:dev");
                }
                
                pb.directory(frontendDir);
                pb.inheritIO();
                
                Map<String, String> env = pb.environment();
                env.put("EXTERNAL_SERVER", "true");
                
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
