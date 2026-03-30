import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Init_DB;
import com.DMHelper.basic.menus.Main_Menu;
import com.DMHelper.basic.menus.Ui_Theme;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.IOException;
import java.io.InputStream;

/**
 * 桌面应用启动入口。
 * 这里负责做三件事：初始化平台参数、启动 Swing UI、以及在启动失败时给出可见的错误提示。
 */
public class Main {
    private static final String APP_ICON_RESOURCE = "/com/DMHelper/assets/app_icon.png";
    private static Image cachedAppIcon;

    public static void main(String[] args) {
        configure_platform_settings();
        install_global_exception_handler();

        SwingUtilities.invokeLater(() -> {
            try {
                configure_look_and_feel();
                boot_application();
            } catch (Throwable throwable) {
                show_startup_error(throwable);
            }
        });
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

    private static void configure_look_and_feel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Ui_Theme.install_global_theme();
    }

    private static void install_global_exception_handler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> show_startup_error(throwable));
    }

    private static void boot_application() {
        Init_DB.setup_database();
        Custom_Equipment_DAO.load_all_custom_items();
        Character_DAO.load_all_characters();
        apply_application_icon();

        Main_Menu menu = new Main_Menu();
        menu.setVisible(true);
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
}
