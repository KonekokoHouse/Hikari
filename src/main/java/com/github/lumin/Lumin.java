package com.github.lumin;

import com.github.lumin.managers.Managers;
import com.github.lumin.utils.AuthUtils;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                            O\ = /O
//                        ____/`---'\____
//                      .   ' \\| |// `.
//                       / \\||| : |||// \
//                     / _||||| -:- |||||- \
//                       | | \\\ - /// | |
//                     | \_| ''\---/'' | |
//                      \ .-\__ `-` ___/-. /
//                   ___`. .' /--.--\ `. . __
//                ."" '< `.___\_<|>_/___.' >'"".
//               | | : `- \`.;`\ _ /`;.`/ - ` : | |
//                 \ \ `-. \_ __\ /__ _/ .-` / /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='

//         .............................................
//                  佛祖保佑             永无BUG
//          佛曰:
//                  写字楼里写字间，写字间里程序员；
//                  程序人员写程序，又拿程序换酒钱。
//                  酒醒只在网上坐，酒醉还来网下眠；
//                  酒醉酒醒日复日，网上网下年复年。
//                  但愿老死电脑间，不愿鞠躬老板前；
//                  奔驰宝马贵者趣，公交自行程序员。
//                  别人笑我忒疯癫，我笑自己命太贱；
//                  不见满街漂亮妹，哪个归得程序员？

// 程序出Bug了？
// 　　　∩∩
// 　　（´･ω･）
// 　 ＿|　⊃／(＿＿_
// 　／ └-(＿＿＿／
// 　￣￣￣￣￣￣￣
// 算了反正不是我写的
// 　　 ⊂⌒／ヽ-、＿
// 　／⊂_/＿＿＿＿ ／
// 　￣￣￣￣￣￣￣
// 万一是我写的呢
// 　　　∩∩
// 　　（´･ω･）
// 　 ＿|　⊃／(＿＿_
// 　／ └-(＿＿＿／
// 　￣￣￣￣￣￣￣
// 算了反正改了一个又出三个
// 　　 ⊂⌒／ヽ-、＿
// 　／⊂_/＿＿＿＿ ／
// 　￣￣￣￣￣￣￣

/**
 * 江城子 . 程序员之歌
 * <p>
 * 十年生死两茫茫，写程序，到天亮。
 * 千行代码，Bug何处藏。
 * 纵使上线又怎样，朝令改，夕断肠。
 * <p>
 * 领导每天新想法，天天改，日日忙。
 * 相顾无言，惟有泪千行。
 * 每晚灯火阑珊处，夜难寐，加班狂。
 */

@Mod(value = Lumin.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public class Lumin {
    public static final String MODID = "lumin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static int skipTicks;

    @SubscribeEvent
    private static void onClientSetup(FMLClientSetupEvent event) {

        AuthUtils.checkConnection();

        LOGGER.info("Welcome to Lumin, Meow~");

        Managers.initManagers();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Managers.CONFIG.saveNow();
            Lumin.LOGGER.info("お兄ちゃん、私はあなたを一番愛しています~");
        }));

        Lumin.LOGGER.info("Lumin has loaded successfully, Meow~");

    }

}
