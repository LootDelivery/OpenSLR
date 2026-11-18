 package java.game;

import java.io.*;
import java.util.*;
import java.util.resource.*;
import java.render.*;   //Text
import java.render.osd.*;       //Text
import java.render.osd.dialog.*;        //Text
import java.sound.*;

public class OptionsDialog extends Dialog
{
        final static ResourceRef        RID_GENERALBG = new ResourceRef( frontend:0x0016r );
        
        // commands
        final static int        CMD_OPTIONS = 0;
        final static int        CMD_VIDEO_OPTIONS = 1;
        final static int        CMD_CONTROL_OPTIONS = 2;
        final static int        CMD_SOUND_OPTIONS = 3;
        final static int        CMD_REDEFINE_CONTROLS = 4;
        final static int        CMD_MAIN = 5;
        final static int        CMD_REDEFINE_DONE = 6;
        final static int        CMD_RESET_CONTROLS = 7;
        final static int        CMD_LOAD_CONTROLS = 8;
        final static int        CMD_SAVE_CONTROLS = 9;
        final static int        CMD_RESOLUTION = 10;
        final static int        CMD_VIDEO_OPTIONS_DONE = 11;
        final static int        CMD_TEXTURE_DETAIL = 12;
        final static int        CMD_SHADOW_DETAIL = 13;
        final static int        CMD_VIEW_RANGE = 14;
        final static int        CMD_EFFECTS_VOL = 15;
        final static int        CMD_MUSIC_VOL = 16;
        final static int        CMD_ENGINE_VOL = 17;
        final static int        CMD_GAME_OPTIONS = 18;
        final static int        CMD_DIFFICULTY = 19;
        final static int        CMD_TRAFFICDENSITY = 20;
        final static int        CMD_PEDESTRIANDENSITY = 21;
        final static int        CMD_CLUTCH = 22;
        final static int        CMD_STEERHELP = 23;
        final static int        CMD_GAME_OPTIONS_DONE = 24;
        final static int        CMD_MOUSE_SENS = 25;

        final static int        CMD_REDEFINE_CONTROLS2 = 26;
        final static int        CMD_REDEFINE_CONTROLS3 = 27;
        final static int        CMD_METRIC = 28;
        final static int        CMD_GAMMA = 29;
        final static int        CMD_OBJECT_DETAIL = 30;
        final static int        CMD_LOD_DETAIL = 31;
        final static int        CMD_ABS_SLIDER = 32;
        final static int        CMD_ASR_SLIDER = 33;
        final static int        CMD_GPSMODE = 34;
        final static int        CMD_AXISCHECK = 35;
        final static int        CMD_AXISCHECK_DONE = 36;
        final static int        CMD_UNUSED = 37;
        final static int        CMD_SOUND_3D = 38;
        final static int        CMD_SOUND_HW = 39;
        final static int        CMD_GAME_OPTIONS2 = 40;
        final static int        CMD_HMF_1 = 41;
        final static int        CMD_HMF_2 = 42;
        final static int        CMD_HMF_3 = 43;
        final static int        CMD_PARTICLE = 44;
        final static int        CMD_HEADLIGHTS = 45;
        final static int        CMD_FLARES = 46;
        final static int        CMD_TTF_SCALE = 47;
        final static int        CMD_TTF_COLOR = 429;
        
        final static int        CMD_SKIDMARK = 402;
        final static int        CMD_SHADOW_RANGE = 403;
        final static int        CMD_REFRESH_TEXT = 404;
        final static int        CMD_MIRROR_DISTANCE = 405;
        final static int        CMD_DEPTH = 406;
        final static int        CMD_MODE = 407;
        final static int        CMD_UNRES = 408;
        final static int        CMD_MIRROR = 409;
        final static int        CMD_BGSCENE = 410;
        final static int        CMD_M_GROUP = 411;
        final static int        CMD_LP = 412;
        final static int        CMD_RED_SUSP = 413;
        final static int        CMD_ARCADE = 414;
        final static int        CMD_MAPFOG = 415;
        final static int        CMD_DAYRACERS = 416;
        final static int        CMD_NIGHTRACERS = 417;
        final static int        CMD_DRIVERMODELS = 418;
        final static int        CMD_FIXED_CONTROLS = 419;
        final static int        CMD_FIXED_CONTROLS_DONE = 420;
        final static int        CMD_INTERFACE = 421;
        final static int        CMD_INTERFACE_DONE = 422;
        final static int        CMD_FONT_OPTIONS = 430;
        final static int        CMD_FONT_OPTIONS_DONE = 431;
        final static int        CMD_TTF_FAMILY = 432;
        final static int        CMD_TTF_STYLE = 433;
        final static int        CMD_TTF_PROFILE_FAMILY_CONSOLE = 434;
        final static int        CMD_TTF_PROFILE_STYLE_CONSOLE = 435;
        final static int        CMD_TTF_PROFILE_FAMILY_SIMPLE = 436;
        final static int        CMD_TTF_PROFILE_STYLE_SIMPLE = 437;
        final static int        CMD_TTF_PROFILE_FAMILY_SLII17 = 438;
        final static int        CMD_TTF_PROFILE_STYLE_SLII17 = 439;
        final static int        CMD_FONT_SCALE_OPTIONS = 440;
        final static int        CMD_FONT_SCALE_OPTIONS_DONE = 441;
        final static int        CMD_TTF_SCALE_SL14 = 442;
        final static int        CMD_TTF_SCALE_SL28 = 443;
        final static int        CMD_TTF_SCALE_SLII11 = 444;
        final static int        CMD_TTF_SCALE_SLII24 = 445;
        final static int        CMD_TTF_SCALE_CONSOLE5 = 446;
        final static int        CMD_TTF_SCALE_CONSOLE10 = 447;
        final static int        CMD_TTF_SCALE_SIMPLE20 = 448;
        final static int        CMD_TTF_SCALE_SIMPLE40 = 449;
        final static int        CMD_TTF_SCALE_SLII17 = 450;
        final static int        CMD_TUNEABLEDOORS = 423;
        final static int        CMD_INV_AUTOSCROLL = 424;
        final static int        CMD_INV_GRIDSIZE = 425;
        final static int        CMD_CATALOGSTYLE = 426;
        final static int        CMD_CARCATCOLOR = 427;
        final static int        CMD_SWAYBARMODE = 428;

        // Font families, merged from the old bitmap font types:
        // sl (sl14/sl28/slii11/slii24), console (console5/console10),
        // simple (simple20/simple40), slii17 (unique). Each family is a TTF
        // in Fonts/families/; picking one re-copies it over Fonts/UI.ttf
        // (applies after game restart, see readme).
        final static String[]   fontFamilyIds = new String[4];
        final static String[]   fontFamilyCaptions = new String[4];
        final static String[]   fontFamilyStyleSuffix = new String[4];
        static int              fontFamilyArraysInited = 0;

        final static int        CMD_GETKEY = 100;
        final static int        CMD_DEAD_ZONE = 200;
        final static int        CMD_POWER = 300;
        final static int        CMD_FFB = 400;

        final static int        NCONTROLS = ControlSet.NCONTROLS;

        static String[] soundHWText;
        static String[] TextureSizeText;
        static String[] ShadowSizeText;
        static String[] LP_TexList;
        static String[] detailTextOO;
        static String[] metricSystemText;
        static String[] gpsText = new String[4];
        static String[] drift_list = new String[4];
        static String[] swaybarList = new String[3];
        static String[] enabledDisabledText;
        static String[] invGridSizeText;
        static String[] catalogStyleText;
        static String[] TTFColorText;
        static int[]    ResX_list, ResY_list;
        
        final static float      aspectX, aspectY;
        String                  Res_Aspect;
        int                     Current_Resolution, aspect, color_depth, windowed, loading_primary, tmp_lp, drift_mode, tmp_drft, tmp_fog, tmp_dayr, tmp_nightr, tmp_driver, tmp_doors, tmp_gridsize, tmp_swaybars;

        Button                  Resolution_button, MirrorDist_Text, Dist_Text, Gamma_Text, ShadowDist_Text, Particle_Text, SkidMark_Text, Depth_button, windowed_button, Arcade_Text;

        Player                  player;

        int                             getKey = -1;
        int                             getName = -1;
        int                             getKeyStat = 0;

        Text[]                  keyText = new Text[NCONTROLS];

        String[]                clutchText = new String[4];
        int[]                   clutchTrf, clutchTrfInv;

        Text                    accelerateText, brakeText, steeringText;

        int                             axisCheckCallerGroup;

        int                             mainGroup, optionsGroup, videoGroup, soundGroup, controlGroup, gameGroup, game2Group, MirrorGroup;
        int                             redefineSuspGroup, redefineGroup, redefine2Group, redefine3Group, axisCheckGroup, fixedCtrlsGroup;
        int                             interfaceGroup;
        int                             fontGroup;

        static String[]         fontFamilyChoices;
        static String[]         fontStyleChoices;
        static int              fontFamilyChoiceCount;

        int                             actGroup;

        int                             textureDetail;
        int                             shadow_size;
        int                             shadow_distance;

        MultiChoice             soundHWMulti;

        Slider[]                powerSlider;
        Slider[]                dzSlider;
        Slider[]                ffbSlider;

        Slider                  acGasSlider, acBreakSlider, acSteerSlider, acClutchSlider, TextureSlider;

        
        ControlSet              controlSet;

        Style butt0 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST );
        Style butt1 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST );
        Style butt2 = new Style( 0.45, 0.12, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST );

        Style buttX = new Style( 0.1, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST );

        Style sld1 = new Style( 0.45, 0.06, Frontend.mediumFont, Text.ALIGN_LEFT, new ResourceRef(Osd.RID_SLD_BACK) );
        Style sldk =  new Style( 0.04, 0.05, Frontend.mediumFont, Text.ALIGN_RIGHT, new ResourceRef(Osd.RID_SLD_KNOB) );
        Style sld2 = new Style( 0.8, 0.06, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(Osd.RID_SLD_BACK) );

        Style Menu_Left = new Style( 0.6, 0.1, Frontend.mediumFont, Text.ALIGN_LEFT, Osd.RRT_TEST );
        Style Menu_Right = new Style( 0.6, 0.1, Frontend.mediumFont, Text.ALIGN_RIGHT, Osd.RRT_TEST );
        Style Menu_Center = new Style( 0.6, 0.1, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST );

        Style Menu_Left_Small = new Style( 0.6, 0.1, Frontend.smallFont, Text.ALIGN_LEFT, Osd.RRT_TEST );
        Style Menu_Right_Small = new Style( 0.6, 0.1, Frontend.smallFont, Text.ALIGN_RIGHT, Osd.RRT_TEST );
        Style Menu_Center_Small = new Style( 0.6, 0.1, Frontend.smallFont, Text.ALIGN_CENTER, Osd.RRT_TEST );

        public OptionsDialog( int additionalFlags )
        {
                super( GameLogic.player.controller, additionalFlags|DF_FULLSCREEN|DF_LOWPRI, null, null );

                float key = Math.random()*100;
                if( MainMenu.Check(key) != Math.sqrt(key)+3.75 ) Mirans_Mod_protection_error______Please_Resintal_Mirans_Mod();

                player=GameLogic.player;

                osd.globalHandler = this;

                actGroup = -1;

                ResX_list = new int[8];
                ResX_list[0] = 1152;
                ResX_list[1] = 1280;
                ResX_list[2] = 1360;
                ResX_list[3] = 1366;
                ResX_list[4] = 1600;
                ResX_list[5] = 1776;
                ResX_list[6] = 1920;
                ResX_list[7] = 2560;

                ResY_list = new int[8];
                ResY_list[0] = 648;
                ResY_list[1] = 720;
                ResY_list[2] = 768;
                ResY_list[3] = 768;
                ResY_list[4] = 900;
                ResY_list[5] = 1000;
                ResY_list[6] = 1080;
                ResY_list[7] = 1440;

                detailTextOO = new String[2];
                detailTextOO[0] = "OFF";                
                detailTextOO[1] = "ON"; 
                
                LP_TexList = new String[3];
                LP_TexList[0] = "LOW";          
                LP_TexList[1] = "MEDIUM";
                LP_TexList[2] = "HIGH";
                
                TextureSizeText = new String[8];
                TextureSizeText[0] = "64 PTX";
                TextureSizeText[1] = "128 PTX";
                TextureSizeText[2] = "256 PTX";
                TextureSizeText[3] = "512 PTX";
                TextureSizeText[4] = "1024 PTX";
                TextureSizeText[5] = "2048 PTX";
                TextureSizeText[6] = "4096 PTX";
                TextureSizeText[7] = "8192 PTX";

                ShadowSizeText = new String[8];
                ShadowSizeText[0] = "OFF";
                ShadowSizeText[1] = "128 PTX";
                ShadowSizeText[2] = "256 PTX";
                ShadowSizeText[3] = "512 PTX";
                ShadowSizeText[4] = "1024 PTX";
                ShadowSizeText[5] = "2048 PTX";
                ShadowSizeText[6] = "4096 PTX";
                ShadowSizeText[7] = "8192 PTX";

                //
                soundHWText = new String[4];
                soundHWText[0] = "SOFTWARE (SLOW!)";
                soundHWText[1] = "HARDWARE (2D ONLY)";
                soundHWText[2] = "HARDWARE (FULL 3D)";
                soundHWText[3] = "AUTO";

                //
                clutchText[0] = "Auto+Autoclutch";
                clutchText[1] = "Semiauto+Autoclutch";
                clutchText[2] = "Manual+Autoclutch";
                clutchText[3] = "Manual+Clutch";

                clutchTrf = new int[6]; //player_transmission values -> clutch mode name indexes
                clutchTrf[0] = 2;
                clutchTrf[1] = 0;
                clutchTrf[2] = 3;
                clutchTrf[3] = -1;
                clutchTrf[4] = -1;
                clutchTrf[5] = 1;

                clutchTrfInv = new int[4];      //clutch mode name indexes -> player_transmission values
                clutchTrfInv[0] = 1;
                clutchTrfInv[1] = 5;
                clutchTrfInv[2] = 0;
                clutchTrfInv[3] = 2;

                metricSystemText = new String[2];
                metricSystemText[0] = "Miles";
                metricSystemText[1] = "Kilometres";

                gpsText[0] = "FOLLOW CAR, MAP RELATIVE";
                gpsText[1] = "FOLLOW CAR, CAR RELATIVE";
                gpsText[2] = "FOLLOW CAMERA, MAP RELATIVE";
                gpsText[3] = "FOLLOW CAMERA, CAMERA RELATIVE";

                drift_list[0] = "OFF";          
                drift_list[1] = "MEDIUM";
                drift_list[2] = "HIGH";
                drift_list[3] = "POWERSLIDE";

                swaybarList[0] = "DEFAULT";             
                swaybarList[1] = "WHEN INSTALLED";
                swaybarList[2] = "ALWAYS WORKING";

                enabledDisabledText = new String[2];
                enabledDisabledText[0] = "DISABLED";
                enabledDisabledText[1] = "ENABLED";

                invGridSizeText = new String[3];
                invGridSizeText[0] = "1x8";
                invGridSizeText[1] = "2x9";
                invGridSizeText[2] = "2x19";

                catalogStyleText = new String[2];
                catalogStyleText[0] = "DEFAULT";
                catalogStyleText[1] = "SLRR LE";

                TTFColorText = new String[5];
                TTFColorText[0] = "DEFAULT";
                TTFColorText[1] = "ICE BLUE";
                TTFColorText[2] = "MINT GREEN";
                TTFColorText[3] = "AMBER";
                TTFColorText[4] = "VIOLET";
        }

        public void show()
        {
        // protection
                float key = Math.random()*100;
                if( MainMenu.Check(key) != Math.sqrt(key)+3.75 )
                {
                        new WarningDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "PROTECTION ERROR", "Miran Wichur Mod protection error: \n \n CORRUPTED FILES \n \n Sorry game will be close. \n Please reinstall Miran Wichur Mod." ).display();
                        GameLogic.changeActiveSection( null );
                }
        
        // find the settings
                aspectX = Config.video_x;
                aspectY = Config.video_y;
                color_depth = Config.video_depth;
                windowed = Config.video_windowed;

        // Look for current resolution mode
                for (int i=0; i<=ResX_list.length-1; i++ ) 
                {
                        if( aspectX == ResX_list[i] ) // identical only X size (used UNCOMMON RES, or similar mode)
                        {
                                Current_Resolution = i; 

                                if(aspectX == ResX_list[i]) // found identical mode
                                {
                                        Current_Resolution = i;
                                        i += ResX_list.length; //end 'for' loop
                                }
                        }
                }

        // Set current texture size
                textureDetail = 4-Config.texture_size; // if 5=>0, if 2=>3 ...

        // Set loading pimary

                if( Config.resource_refreshrate == 512 )
                        loading_primary = 0;
                else
                if( Config.resource_refreshrate == 2048 )
                        loading_primary = 1;
                else
                if( Config.resource_refreshrate == 12288 )
                        loading_primary = 2;

                tmp_lp = loading_primary;

                // Set drift mode

                if( Config.ground_friction == 1.0 )
                        drift_mode = 0;
                else
                if( Config.ground_friction == 0.8 )
                        drift_mode = 1;
                else
                if( Config.ground_friction == 0.6 )
                        drift_mode = 2;
                else
                if( Config.ground_friction == 1.4 )
                        drift_mode = 3;

                tmp_drft = drift_mode;
                tmp_fog = Config.mapFog;
                tmp_dayr = Config.dayRacers;
                tmp_nightr = Config.nightRacers;
                tmp_driver = Config.driverModel;
                tmp_doors = Config.tuneableDoors;
                tmp_gridsize = Config.invGridSize;
                tmp_swaybars = Config.swaybarsMode;

        // Set current shadow size
                if( Config.shadow_size == 8192 )
                        shadow_size = 7;
                else
                if( Config.shadow_size == 4096 )
                        shadow_size = 6;
                else
                if( Config.shadow_size == 2048 )
                        shadow_size = 5;
                else
                if( Config.shadow_size == 1024 )
                        shadow_size = 4;
                else
                if( Config.shadow_size == 512 )
                        shadow_size = 3;
                else
                if( Config.shadow_size == 256 )
                        shadow_size = 2;
                else
                if( Config.shadow_size == 128 )
                        shadow_size = 1;
                else
                if( Config.shadow_size < 64 )
                        shadow_size = 0;

                float x, y, spc;
                float lpos = -1.0, mpos = -0.10;

                //--------------main menu--------------
                addCustomGroups();

                //--------------options menu--------------
                Menu m;

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "OPTIONS" );

                m = osd.createMenu( butt1, 0.0, -0.6, 0 );
                m.addItem( "VIDEO OPTIONS", CMD_VIDEO_OPTIONS );
                m.addItem( "SOUND OPTIONS", CMD_SOUND_OPTIONS );
                m.addItem( "CONTROL OPTIONS", CMD_CONTROL_OPTIONS );
                m.addItem( "INTERFACE OPTIONS", CMD_INTERFACE );
                m.addItem( "FONT OPTIONS", CMD_FONT_OPTIONS );
                m.addItem( "GAME OPTIONS", CMD_GAME_OPTIONS );
                m.addItem( "ADVANCED GAME OPTIONS", CMD_GAME_OPTIONS2 );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_MAIN );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_MAIN, this );
                osd.hideGroup( optionsGroup = osd.endGroup() );

                //-----------video options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "VIDEO OPTIONS" );

                // Left side:
                m = osd.createMenu( Menu_Center, -0.5, -0.5, 0 );
                Resolution_button = m.addItem( "SCREEN RESOLUTION: --- X --- (---)", CMD_RESOLUTION, "Get next standard resolution" );

                m.addItem( "UNCOMMON RESOLUTION", CMD_UNRES, "Set uncommon resolution" );
                windowed_button = m.addItem( "DISPLAY MODE: ---", CMD_MODE, null );
                
                Depth_button = m.addItem( "COLOR DEPTH: ---", CMD_DEPTH, null );

                m = osd.createMenu( Menu_Left, -0.5, 0.0, 0 );
                m.addItem( "HEADLIGHT RAYS:", CMD_HEADLIGHTS, Config.headlight_rays, detailTextOO, null );

                m.addItem( "FLARES:", CMD_FLARES, Config.flares, detailTextOO, null );
//              m.addItem( "MENU 3D SCENE:", CMD_BGSCENE, Config.BgScene, detailTextOO, null );
//              m.addItem( "MIRROR OPTIONS", CMD_M_GROUP, "Settings of mirror" );
                
                m.addItem( "LOADING PRIMARY:", CMD_LP, loading_primary, LP_TexList, "Use LOW to fast game working" );

                m.addItem( "TEXTURE DETAIL", CMD_TEXTURE_DETAIL, textureDetail, TextureSizeText, null );

                m.addItem( "SHADOW DETAIL", CMD_SHADOW_DETAIL, shadow_size, ShadowSizeText, "Max shadow texture size" );


                //m = osd.createMenu( Menu_Left, 0.5, -0.5, 0 );
                //m.setSliderStyle( sld1, sldk );
                //m.addItem( "GAMMA", CMD_GAMMA, Config.video_gamma, 0.4, 4.0, 0, "Max shadow texture size" ).printValue( "%.2f" );


                // Right side:
                m = osd.createMenu( Menu_Center, 0.5, -0.45, 0.15 );

                Gamma_Text = m.addItem( "GAMMA: " + Float.toString(Config.video_gamma, "%0.2f"), CMD_UNUSED, "Cofigure video gamma." );

                m.addItem( "OBJECT DETAIL", CMD_UNUSED, "How much details is visible" );

                m.addItem( "LOD DETAIL", CMD_UNUSED, "Distance of objects simplification" );

                Dist_Text = m.addItem( "VIEW DISTANCE: " + Float.toString(Config.cameras_viewrange, "%.0f m"), CMD_UNUSED, null );

                ShadowDist_Text = m.addItem( "SHADOW VIEW DISTANCE: " + Float.toString(1/Config.shadow_detail*10, "%.0f m"), CMD_UNUSED, "Max shadow view distance" );

                SkidMark_Text = m.addItem( "SKIDMARKS LENGTH: "+Config.skidmark_max/4+" m", CMD_UNUSED, "Max length of skidmarks" );

                Particle_Text = m.addItem( "PARTICLE DENSITY: " + Float.toString(Config.particle_density*100, "%.0f")+"%", CMD_UNUSED, "Density of tyres smoke, sparks etc." );



                m = osd.createMenu( Menu_Center, 0.5, -0.5, 0.15 );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_GAMMA, Config.video_gamma, 0.4, 4.0, 0, null );//.printValue( "%.2f" );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_OBJECT_DETAIL, Config.object_detail, 0.074, 0.01, 11, "How much details is visable" );//.printValue( "%2f" );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_LOD_DETAIL, Config.object_detail_amp, 35.0, 0.2, 11, "Distance of objects simplification" );//.printValue( "%.1f" );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_VIEW_RANGE, Config.cameras_viewrange, 100, 10000, 199, null );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_SHADOW_RANGE, 1/Config.shadow_detail*10, 10, 1000+0.001, 100, "Max shadow view distance" );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_SKIDMARK, Config.skidmark_max/4, 50, 1000, 20, "Max length of skidmarks" );//.printValue( "%.0f m" );

                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_PARTICLE, Config.particle_density, 0.0, 5.0, 11, "Density of tyres smoke, sparks etc." );//.printValue( "%.2f" );

                
                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_VIDEO_OPTIONS_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_VIDEO_OPTIONS_DONE, this );
                osd.hideGroup( videoGroup = osd.endGroup() );

        // Mirror setings               
                //osd.createBG( RID_GENERALBG );
                /*osd.createHeader( "MIRROR OPTIONS" );

                m = osd.createMenu( Menu_Left, -0.5, 0.5, 0 );
                m.addItem( "MIRROR:", CMD_MIRROR, Config.mirror, detailTextOO, null );

                m = osd.createMenu( Menu_Center_Small, -0.5, -0.05, 0 );
                MirrorDist_Text = m.addItem( "VIEW DISTANCE: " + Float.toString(Config.camera_mirror_viewrange, "%.0f m"), CMD_UNUSED, null );
                m = osd.createMenu( Menu_Left_Small, -0.5, 0.0, 0 );
                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_MIRROR_DISTANCE, Config.camera_mirror_viewrange, 5, 105, 41, null );

                m = osd.createMenu( Menu_Center_Small, -0.5, 0.10, 0 );
                m.addItem( "OBJECT DETAIL", CMD_UNUSED, "How much details is visable" );
                m = osd.createMenu( Menu_Left_Small, -0.5, 0.15, 0 );
                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_OBJECT_DETAIL, Config.object_detail, 0.074, 0.01, 11, "How much details is visable" );//.printValue( "%2f" );

                m = osd.createMenu( Menu_Center_Small, -0.5, 0.25, 0 );
                m.addItem( "LOD DETAIL", CMD_UNUSED, "Distance of objects simplification" );
                m = osd.createMenu( Menu_Left_Small, -0.5, 0.30, 0 );
                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_LOD_DETAIL, Config.object_detail_amp, 35.0, 4.0, 11, "Distance of objects simplification" );//.printValue( "%.1f" );

                // right side
                m = osd.createMenu( Menu_Left, 0.6, -0.04, 0 );
                m.addItem( "SHADOW DETAIL", CMD_SHADOW_DETAIL, shadow_size, ShadowSizeText, "Max shadow texture size" );

                m = osd.createMenu( Menu_Center_Small, 0.5, 0.10, 0 );
                m.addItem( "SHADOW VIEW DISTANCE: " + Float.toString(1/Config.shadow_detail*10, "%.0f m"), CMD_UNUSED, "Max shadow view distance" );
                m = osd.createMenu( Menu_Left_Small, 0.5, 0.15, 0 );
                m.setSliderStyle( sld2, sldk );
                m.addItem( null, CMD_SHADOW_RANGE, 1/Config.shadow_detail*10, 10, 210, 21, "Max shadow view distance" );

                m = osd.createMenu( butt2, 0.98, 0.85, 0 );
                m.addItem( "BACK", CMD_VIDEO_OPTIONS );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_VIDEO_OPTIONS, this );
                osd.hideGroup( MirrorGroup = osd.endGroup() );*/


                //-----------sound options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "SOUND OPTIONS" );

                m = osd.createMenu( butt0, 0.0, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                m.addItem( "EFFECTS VOLUME", CMD_EFFECTS_VOL, Sound.getVolume(Sound.CHANNEL_EFFECTS), null );
                m.addItem( "MUSIC VOLUME", CMD_MUSIC_VOL, Sound.getVolume(Sound.CHANNEL_MUSIC), null );
                m.addItem( "ENGINE VOLUME", CMD_ENGINE_VOL, Sound.getVolume(Sound.CHANNEL_ENGINE), null );
                m.addSeparator();
                soundHWMulti = m.addItem( "MIXING", CMD_SOUND_HW, 0, soundHWText, null );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_OPTIONS );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_OPTIONS, this );
                osd.hideGroup( soundGroup = osd.endGroup() );

                //---------controller options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "CONTROL OPTIONS" );

                m = osd.createMenu( butt1, 0.0, -0.6, 0 );

                m.addItem( "REDEFINE CAR CONTROLS", CMD_REDEFINE_CONTROLS );
                m.addItem( "REDEFINE GAME CONTROLS", CMD_REDEFINE_CONTROLS2 );
                m.addItem( "FIXED GAME CONTROLS", CMD_FIXED_CONTROLS );
                m.addItem( "ADVANCED SETTINGS", CMD_REDEFINE_CONTROLS3 );
                m.addItem( "RESET TO DEFAULTS", CMD_RESET_CONTROLS );
                m.addItem( "LOAD CONTROLS", CMD_LOAD_CONTROLS );
                m.addItem( "SAVE CONTROLS", CMD_SAVE_CONTROLS );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_OPTIONS );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_OPTIONS, this );
                osd.hideGroup( controlGroup = osd.endGroup() );

                //-------car controller options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "REDEFINE CAR CONTROLS - DRIVING" );

                m = osd.createMenu( butt0, -0.95, -0.83, 0,  Osd.MD_HORIZONTAL );
                m.addItem( "DRIVING / SUSPENSION", CMD_RED_SUSP );
                //m.addItem( "DRIVING", CMD_REDEFINE_CONTROLS );
                //m.addItem( "SUSPENSION", CMD_RED_SUSP );

                //m = osd.createMenu( butt0, -0.95, -0.65, 0,  Osd.MD_HORIZONTAL );
                //m.addItem( "KEYBOARD", -1 );
                osd.createText( "KEYBOARD", Frontend.mediumFont, Text.ALIGN_LEFT, -0.915, -0.675 );
                //osd.createText( "    KEYBOARD", Frontend.mediumFont, Text.ALIGN_LEFT, lpos, -0.7 );

                m = osd.createMenu( butt0, -0.95, -0.45, 0 );
                m.addItem( "ACCELERATE", CMD_GETKEY + 0 );
                m.addItem( "BRAKE", CMD_GETKEY + 1 );
                m.addItem( "TURN LEFT", CMD_GETKEY + 2 );
                m.addItem( "TURN RIGHT", CMD_GETKEY + 3 );
                m.addItem( "HAND BRAKE", CMD_GETKEY + 4 );
                m.addItem( "SHIFT UP", CMD_GETKEY + 5 );
                m.addItem( "SHIFT DOWN", CMD_GETKEY + 6 );
                m.addItem( "CLUTCH", CMD_GETKEY + 7 );
                m.addItem( "ENGAGE NOS", CMD_GETKEY + 8 );
                m.addItem( "HONK HORN", CMD_GETKEY + 9 );

                //m = osd.createMenu( butt0, -0.1, -0.65, 0,  Osd.MD_HORIZONTAL );
                //m.addItem( "CONTROLLER", -1 );
                osd.createText( "CONTROLLER", Frontend.mediumFont, Text.ALIGN_LEFT, -0.065, -0.675 );
                //osd.createText( " CONTROLLER", Frontend.mediumFont, Text.ALIGN_LEFT, mpos, -0.7 );

                m = osd.createMenu( butt0, -0.1, -0.45, 0 );
                m.addItem( "ACCELERATE", CMD_GETKEY + 15 );
                m.addItem( "BRAKE", CMD_GETKEY + 16 );
                m.addItem( "TURN LEFT", CMD_GETKEY + 17 );
                m.addItem( "TURN RIGHT", CMD_GETKEY + 18 );
                m.addItem( "HAND BRAKE", CMD_GETKEY + 19 );
                m.addItem( "SHIFT UP", CMD_GETKEY + 20 );
                m.addItem( "SHIFT DOWN", CMD_GETKEY + 21 );
                m.addItem( "CLUTCH", CMD_GETKEY + 22 );
                m.addItem( "ENGAGE NOS", CMD_GETKEY + 23 );
                m.addItem( "HONK HORN", CMD_GETKEY + 24 );

                m = osd.createMenu( butt0, -0.98, 0.95, 0 );
                m.addItem( "CHECK AXES", CMD_AXISCHECK );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_REDEFINE_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_REDEFINE_DONE, this );

                x = -0.40;
                y = -0.48;
                for (int i = 0; i < 25; i++)
                {
                        if( i == 10 )
                        {
                                y += 0.6;
                        }
                        if( i == 15 )
                        {
                                x = 0.50;
                                y = -0.48;
                        }
                        if (i < 10 || i > 14)
                                keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }

                osd.hideGroup( redefineGroup = osd.endGroup() );

                //-------car pneumatis suspension--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "REDEFINE CAR CONTROLS - SUSPENSION" );
                osd.createRectangle( 0.0, 0.0, 1.65, 1.15, -1, new ResourceRef(MW_Mod:0x0097r) );//background car

                m = osd.createMenu( butt0, -0.95, -0.83, 0,  Osd.MD_HORIZONTAL );
                m.addItem( "DRIVING / SUSPENSION", CMD_REDEFINE_CONTROLS );
                //m.addItem( "DRIVING", CMD_REDEFINE_CONTROLS );
                //m.addItem( "SUSPENSION", CMD_RED_SUSP );

                osd.createText( "Redefine pneumatic suspension controls:", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, -0.7 );

                m = osd.createMenu( Menu_Center, 0.54, -0.38, 0 );
                m.addItem( "FL", CMD_GETKEY + 65 );
                keyText[65] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.54, -0.32);

                m = osd.createMenu( Menu_Center, 0.54, -0.03, 0 );
                m.addItem( "Front", CMD_GETKEY + 66 );
                keyText[66] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.54, 0.03);

                m = osd.createMenu( Menu_Center, 0.54, 0.32, 0 );
                m.addItem( "FR", CMD_GETKEY + 67 );
                keyText[67] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.54, 0.38);

                m = osd.createMenu( Menu_Center, 0.0, -0.38, 0 );
                m.addItem( "Left", CMD_GETKEY + 68 );
                keyText[68] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, -0.32);

                m = osd.createMenu( Menu_Center, 0.0, -0.03, 0 );
                m.addItem( "All", CMD_GETKEY + 69 );
                keyText[69] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.03);

                m = osd.createMenu( Menu_Center, 0.0, 0.32, 0 );
                m.addItem( "Right", CMD_GETKEY + 70 );
                keyText[70] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, 0.0, 0.38);

                m = osd.createMenu( Menu_Center, -0.54, -0.4, 0 );
                m.addItem( "RL", CMD_GETKEY + 71 );
                keyText[71] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, -0.54, -0.32);

                m = osd.createMenu( Menu_Center, -0.54, -0.03, 0 );
                m.addItem( "Rear", CMD_GETKEY + 72 );
                keyText[72] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, -0.54, 0.03);

                m = osd.createMenu( Menu_Center, -0.54, 0.32, 0 );
                m.addItem( "RR", CMD_GETKEY + 73 );
                keyText[73] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_CENTER, -0.54, 0.38);

                m = osd.createMenu( Menu_Right, 0.2, 0.7, 0 );
                m.addItem( "Change Mode (Up/Down)", CMD_GETKEY + 74 );
                keyText[74] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, 0.22, 0.66);

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_REDEFINE_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_REDEFINE_DONE, this );

                osd.hideGroup( redefineSuspGroup = osd.endGroup() );

                //-------game controller options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "REDEFINE GAME CONTROLS" );

                //m = osd.createMenu( butt0, -0.95, -0.60, 0 );
                m = osd.createMenu( butt0, -0.35, -0.60, 0 );
                m.addItem( "PAUSE", CMD_GETKEY + 78 );
                m.addItem( "NEXT RADIO STATION", CMD_GETKEY + 11 );
                m.addItem( "PREV RADIO STATION", CMD_GETKEY + 12 );
                m.addItem( "VOLUME UP", CMD_GETKEY + 13 );
                m.addItem( "VOLUME DOWN", CMD_GETKEY + 14 );
                m.addItem( "CRUISE CONTROL", CMD_GETKEY + 75 );
                m.addItem( "LOOK BACK", CMD_GETKEY + 76 );
                m.addItem( "VIEW MAP", CMD_GETKEY + 77 );
                m.addItem( "SCREENSHOT", CMD_GETKEY + 25 );     
                m.addItem( "ZOOM +", CMD_GETKEY + 63 );
                m.addItem( "ZOOM -", CMD_GETKEY + 64 ); 

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_REDEFINE_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_REDEFINE_DONE, this );

                //x = -0.40;
                x =  0.25;
                y = -0.62;
                for (int i = 78; i <= 78; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }
                for (int i = 11; i <= 14; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }
                for (int i = 75; i <= 77; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }
                for (int i = 25; i <= 25; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }
                for (int i = 63; i <= 64; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }

                //x =  0.40;
                //y = -0.48;
                //for (int i = 78; i <= 79; i++)
                //{
                //      keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                //      y += 0.12;
                //}

                /*x =  0.30;
                y = -0.63;
                for (int i = 11; i <= 14; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }

                y = -0.09;
                for (int i = 75; i <= 77; i++)
                {
                        keyText[i] = osd.createText ("Undefined", Frontend.mediumFont, Text.ALIGN_LEFT, x, y);
                        y += 0.12;
                }*/

                osd.hideGroup( redefine2Group = osd.endGroup() );

                //-------game controller options menu--------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "FIXED GAME CONTROLS" );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_FIXED_CONTROLS_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FIXED_CONTROLS_DONE, this );

                x = -0.315;
                y = -0.620;
                osd.createText( "SHOW/HIDE OSD", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );          y += 0.12;
                osd.createText( "SHOW/HIDE GPS", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );      y += 0.12;
                osd.createText( "ON/OFF LIGHTS", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );      y += 0.12;
                osd.createText( "CAR COORDINATES", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );    y += 0.12;
                osd.createText( "LIVE GARAGE", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "QUICK REPAIR", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );       y += 0.12;
                osd.createText( "FREE CAMERA", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "RANDOM CAMERA TILT", Frontend.mediumFont, Text.ALIGN_LEFT, x, y ); y += 0.12;
                osd.createText( "LEFT CAMERA TILT", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );   y += 0.12;
                osd.createText( "RIGHT CAMERA TILT", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );  y += 0.12;
                osd.createText( "ZERO CAMERA TILT", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );   y += 0.12;

                x =  0.250;
                y = -0.620;
                osd.createText( "TAB", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );      y += 0.12;
                osd.createText( "N", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "L", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "P", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "F6 / F10", Frontend.mediumFont, Text.ALIGN_LEFT, x, y ); y += 0.12;
                osd.createText( "F7 / F11", Frontend.mediumFont, Text.ALIGN_LEFT, x, y ); y += 0.12;
                osd.createText( "F9", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );       y += 0.12;
                osd.createText( "7", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "8", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "9", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;
                osd.createText( "0", Frontend.mediumFont, Text.ALIGN_LEFT, x, y );        y += 0.12;

                osd.hideGroup( fixedCtrlsGroup = osd.endGroup() );

                //-------------advanced settings menu----------------

                powerSlider = new Slider[4];
                dzSlider = new Slider[4];
                ffbSlider = new Slider[2];

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "ADVANCED CONTROL SETTINGS" );

                m = osd.createMenu( butt1, 0.0, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                powerSlider[0] = m.addItem( "ACCEL. GAMMA", CMD_POWER + 0, 0.0, null );
                powerSlider[1] = m.addItem( "BRAKE GAMMA", CMD_POWER + 1, 0.0, null );
                powerSlider[2] = m.addItem( "STEERING GAMMA", CMD_POWER + 2, 0.0, null );
                powerSlider[3] = m.addItem( "CLUTCH GAMMA", CMD_POWER + 3, 0.0, null );

                m.addItem( "MOUSE SENSITIVITY", CMD_MOUSE_SENS, Config.mouseSensitivity, null );

                dzSlider[0] = m.addItem( "ACCELERATE DEAD ZONE", CMD_DEAD_ZONE + 0, 0.0, null );
                dzSlider[1] = m.addItem( "BRAKE DEAD ZONE", CMD_DEAD_ZONE + 1, 0.0, null );
                dzSlider[2] = m.addItem( "STEERING DEAD ZONE", CMD_DEAD_ZONE + 2, 0.0, null );
                dzSlider[3] = m.addItem( "CLUTCH DEAD ZONE", CMD_DEAD_ZONE + 3, 0.0, null );
                
                m.addSeparator();
                ffbSlider[0] = m.addItem( "REAL FFB STRENGTH", CMD_FFB + 0, Config.FFB_strength, 0.0f, 1.0f, 0, null );
                ffbSlider[1] = m.addItem( "EMULATED FFB STRENGTH", CMD_FFB + 1, Config.FFB_strength_emulated, 0.0f, 0.1f, 0, null );

                m = osd.createMenu( butt0, -0.98, 0.95, 0 );
                m.addItem( "CHECK AXES", CMD_AXISCHECK );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_REDEFINE_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_REDEFINE_DONE, this );

                osd.hideGroup( redefine3Group = osd.endGroup() );

                //-------------axis checker menu----------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "VIEW CONTROL AXES" );
                osd.createText( "Please review how the axes respond to the car controls:", Frontend.mediumFont, Text.ALIGN_LEFT, -0.95, -0.7 );

                Style sldac = new Style( 0.85, 0.06, Frontend.mediumFont, Text.ALIGN_CENTER, new ResourceRef(Osd.RID_SLD_BACK) );

                m = osd.createMenu( butt1, 0.0, -0.45, 0 );
                m.setSliderStyle( sldac, sldk );

                acGasSlider = m.addItem( "THROTTLE", CMD_UNUSED, 0.0, 0.0, 1.0, 0, null );
                acGasSlider.nofocus = 1;
                m.addSeparator();
                acBreakSlider = m.addItem( "BRAKE", CMD_UNUSED, 0.0, 0.0, 1.0, 0, null );
                acBreakSlider.nofocus = 1;
                m.addSeparator();
                acSteerSlider = m.addItem( "STEER", CMD_UNUSED, 0.5, 0.0, 1.0, 0, null );
                acSteerSlider.nofocus = 1;
                m.addSeparator();
                acClutchSlider = m.addItem( "CLUTCH", CMD_UNUSED, 0.5, 0.0, 1.0, 0, null );
                acClutchSlider.nofocus = 1;

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_AXISCHECK_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_AXISCHECK_DONE, this );

                osd.hideGroup( axisCheckGroup = osd.endGroup() );

                //-------------game options menu----------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "GAME OPTIONS" );

                m = osd.createMenu( butt0, 0.0, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                m.addItem( "TRANSMISSION", CMD_CLUTCH, clutchTrf[Config.player_transmission], clutchText, null );
                m.addItem( "METRIC SYSTEM", CMD_METRIC, Config.metricSystem, metricSystemText, null );
                m.addItem( "GPS MODE", CMD_GPSMODE, Config.gpsMode, gpsText, null );
                m.addItem( "DIFFICULTY LEVEL", CMD_DIFFICULTY, Config.deformation, 0.5, 1.5, 0, null );
                m.addSeparator();
                m.addItem( "STEERING HELP", CMD_STEERHELP, Config.player_steeringhelp, null );
                m.addItem( "ABS", CMD_ABS_SLIDER, Config.player_abs, null );
                m.addItem( "ASR", CMD_ASR_SLIDER, Config.player_asr, null );
                m.addSeparator();
                m.addItem( "DRIFT MODE", CMD_ARCADE, drift_mode, drift_list, "Affects the tire grip. POWERSLIDE stands for higher values than OFF" );
                m.addItem( "SWAYBAR MODE", CMD_SWAYBARMODE, Config.swaybarsMode, swaybarList, "Sets the conditions under which swaybars will affect vehicle handling" );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_GAME_OPTIONS_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_GAME_OPTIONS_DONE, this );
                osd.hideGroup( gameGroup = osd.endGroup() );

                //-------------game options 2 menu----------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "ADVANCED GAME OPTIONS" );

                m = osd.createMenu( butt0, 0.0, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                m.addItem( "DRIVER MOVEMENT/STEER", CMD_HMF_1, Config.head_move_steer, 0.0, 2.0, 0, "How the steering wheels position affects the drivers movement" );
                m.addItem( "DRIVER MOVEMENT/VELOCITY", CMD_HMF_2, Config.head_move_vel, 0.0, 2.0, 0, "How the cars velocity affects the drivers movement"  );
                m.addItem( "DRIVER MOVEMENT/ACCELERATION", CMD_HMF_3, Config.head_move_acc, 0.0, 2.0, 0, "How the cars acceleration affects the drivers movement"  );
                m.addSeparator();
                m.addItem( "TRAFFIC DENSITY", CMD_TRAFFICDENSITY, Config.trafficDensity, 0.0, 5.0, 0, null );
                m.addItem( "PEDESTRIAN DENSITY", CMD_PEDESTRIANDENSITY, Config.pedestrianDensity, 0.0, 1.5, 0, null );
                m.addSeparator();
                m.addItem( "TRACK FOG", CMD_MAPFOG, Config.mapFog, enabledDisabledText, null );
                m.addSeparator();
                m.addItem( "DAYTIME RACERS", CMD_DAYRACERS, Config.dayRacers, enabledDisabledText, null );
                m.addItem( "NIGHTTIME RACERS", CMD_NIGHTRACERS, Config.nightRacers, enabledDisabledText, null );
                m.addSeparator();
                m.addItem( "HUMAN MODELS", CMD_DRIVERMODELS, Config.driverModel, enabledDisabledText, null );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_GAME_OPTIONS_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_GAME_OPTIONS_DONE, this );
                osd.hideGroup( game2Group = osd.endGroup() );

                //-------------game options 3 menu----------------

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "INTERFACE OPTIONS" );

                m = osd.createMenu( butt0, 0.0, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                m.addItem( "INVENTORY AUTOSCROLL", CMD_INV_AUTOSCROLL, Config.invAutoScroll, enabledDisabledText, "Sets the inventory scroll when adding new parts" );
                m.addItem( "INVENTORY GRID SIZE", CMD_INV_GRIDSIZE, Config.invGridSize, invGridSizeText, "Size of the inventory pane grid (rows x columns)" );
                m.addSeparator();
                m.addItem( "ADJUSTABLE DOORS", CMD_TUNEABLEDOORS, Config.tuneableDoors, enabledDisabledText, "Behavior of doors by clicking with the screwdriver" );
                m.addSeparator();
                m.addItem( "CAR COLOR PICKER", CMD_CARCATCOLOR, Config.carCatalogColor, enabledDisabledText, "Sets the ability to change color of the assembled vehicle in catalog" );
                m.addItem( "CATALOG STYLE", CMD_CATALOGSTYLE, Config.catalogStyle, catalogStyleText, "Sets the display of catalog headers and affects on buttons layout" );
                m.addSeparator();
                m.addItem( "INTERFACE FONT COLOR", CMD_TTF_COLOR, Osd.getInterfaceFontColorMode(), TTFColorText, "Changes the OSD/interface text color palette" );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_INTERFACE_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_INTERFACE_DONE, this );
                osd.hideGroup( interfaceGroup = osd.endGroup() );

                //-------------font options menu----------------
                // One page: font pickers on the left, per-resource size
                // sliders on the right (the exe re-reads Fonts/TTF.scale.*
                // during rendering, so slider changes apply on the fly).

                buildFontChoiceArrays();

                osd.createBG( RID_GENERALBG );
                osd.createHeader( "FONT OPTIONS" );

                m = osd.createMenu( butt0, -0.5, -0.6, 0 );
                m.setSliderStyle( sld1, sldk );

                m.addItem( "SL FONT", CMD_TTF_FAMILY, fontFamilyIdToPosition( Osd.getTTFProfileFamily(0) ), fontFamilyChoices, "TTF used by sl14, sl28, slii11 and slii24" );
                m.addItem( "SL STYLE", CMD_TTF_STYLE, Osd.getTTFProfileStyle(0), fontStyleChoices, "Regular / Bold / Italic / Bold Italic for the SL profile" );
                m.addItem( "CONSOLE FONT", CMD_TTF_PROFILE_FAMILY_CONSOLE, fontFamilyIdToPosition( Osd.getTTFProfileFamily(1) ), fontFamilyChoices, "TTF used by console5 and console10" );
                m.addItem( "CONSOLE STYLE", CMD_TTF_PROFILE_STYLE_CONSOLE, Osd.getTTFProfileStyle(1), fontStyleChoices, "Style for the Console profile" );
                m.addItem( "SIMPLE FONT", CMD_TTF_PROFILE_FAMILY_SIMPLE, fontFamilyIdToPosition( Osd.getTTFProfileFamily(2) ), fontFamilyChoices, "TTF used by simple20 and simple40" );
                m.addItem( "SIMPLE STYLE", CMD_TTF_PROFILE_STYLE_SIMPLE, Osd.getTTFProfileStyle(2), fontStyleChoices, "Style for the Simple profile" );
                m.addItem( "SLII17 FONT", CMD_TTF_PROFILE_FAMILY_SLII17, fontFamilyIdToPosition( Osd.getTTFProfileFamily(3) ), fontFamilyChoices, "TTF used by slii17" );
                m.addItem( "SLII17 STYLE", CMD_TTF_PROFILE_STYLE_SLII17, Osd.getTTFProfileStyle(3), fontStyleChoices, "Style for the SLII17 profile" );

                m = osd.createMenu( sld2, 0.5, -0.6, 0 );
                m.setSliderStyle( sld2, sldk );
                m.addItem( "SL28 SIZE", CMD_TTF_SCALE_SL28, Osd.getTTFResourceScale(1), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SLII24 SIZE", CMD_TTF_SCALE_SLII24, Osd.getTTFResourceScale(3), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SLII17 SIZE", CMD_TTF_SCALE_SLII17, Osd.getTTFResourceScale(8), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SLII11 SIZE", CMD_TTF_SCALE_SLII11, Osd.getTTFResourceScale(2), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SL14 SIZE", CMD_TTF_SCALE_SL14, Osd.getTTFResourceScale(0), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "CONSOLE5 SIZE", CMD_TTF_SCALE_CONSOLE5, Osd.getTTFResourceScale(4), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "CONSOLE10 SIZE", CMD_TTF_SCALE_CONSOLE10, Osd.getTTFResourceScale(5), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SIMPLE20 SIZE", CMD_TTF_SCALE_SIMPLE20, Osd.getTTFResourceScale(6), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );
                m.addItem( "SIMPLE40 SIZE", CMD_TTF_SCALE_SIMPLE40, Osd.getTTFResourceScale(7), 60.0, 150.0, 19, null ).printValue( "%.0f%%" );

                m = osd.createMenu( butt2, 0.98, 0.95, 0 );
                m.addItem( "BACK", CMD_FONT_OPTIONS_DONE );

                osd.createHotkey( Input.AXIS_CANCEL, Input.VIRTUAL, CMD_FONT_OPTIONS_DONE, this );
                osd.hideGroup( fontGroup = osd.endGroup() );

                super.show();

                enableAnimateHook();
        }

        public void hide()
        {
                disableAnimateHook();
                super.hide();
        }


        public void addCustomGroups()
        {
        }


//--------------------------------------------------------------------------------------

        // SLRR uses its own java.io.File API: every file it writes gets the
        // SDAT container header, while the exe hook reads Fonts/TTF.scale as
        // RAW plain ASCII ("80") and cannot parse that container. So the
        // percent itself is stored in Fonts/TTF.settings (java side, together
        // with the color palette) and the exe-facing Fonts/TTF.scale is
        // updated by copying one of the prebuilt plain presets from
        // Fonts/scale/ (one file per slider step, never written at runtime).
        // The exe reads TTF.scale once at startup, so a new size applies
        // after the next game start.
        static int getTTFScalePercent()
        {
                return Osd.getInterfaceFontScalePercent();
        }

        static int snapTTFScalePercent( int percent )
        {
                if( percent < 60 ) percent = 60;
                if( percent > 150 ) percent = 150;

                // keep the slider on the shipped preset steps (5%)
                percent = ((percent + 2) / 5) * 5;

                if( percent < 60 ) percent = 60;
                if( percent > 150 ) percent = 150;
                return percent;
        }

        static void setTTFScalePercent( int percent )
        {
                percent = snapTTFScalePercent( percent );

                Osd.setInterfaceFontScalePercent( percent );

                // update the exe-facing plain file from the shipped preset;
                // no delete, no preset files are ever created at runtime
                File.copy( "Fonts/scale/" + Integer.toString( percent ), "Fonts/TTF.scale" );
        }

        static void initFontFamilyArrays()
        {
                if( fontFamilyArraysInited != 0 )
                        return;
                fontFamilyArraysInited = 1;

                fontFamilyIds[0] = "sl";
                fontFamilyIds[1] = "console";
                fontFamilyIds[2] = "simple";
                fontFamilyIds[3] = "slii17";

                fontFamilyCaptions[0] = "SL";
                fontFamilyCaptions[1] = "CONSOLE";
                fontFamilyCaptions[2] = "SIMPLE";
                fontFamilyCaptions[3] = "SLII17";

                fontFamilyStyleSuffix[0] = "";
                fontFamilyStyleSuffix[1] = "-bold";
                fontFamilyStyleSuffix[2] = "-italic";
                fontFamilyStyleSuffix[3] = "-bold-italic";
        }

        // Builds the FONT FAMILY / FONT STYLE choice lists. Families map to
        // the four old bitmap font types; a family is only listed when its
        // base TTF exists in Fonts/families/.
        static void buildFontChoiceArrays()
        {
                initFontFamilyArrays();

                String[] familyNames = new String[fontFamilyIds.length + 1];
                int[] familyIndex = new int[fontFamilyIds.length + 1];
                int count = 1;

                familyNames[0] = "STOCK (UI.ttf)";
                familyIndex[0] = 0;

                for( int i = 0; i < fontFamilyIds.length; i++ )
                {
                        if( File.exists( "Fonts/families/" + fontFamilyIds[i] + ".ttf" ) )
                        {
                                familyNames[count] = fontFamilyCaptions[i];
                                familyIndex[count] = i + 1;
                                count++;
                        }
                }

                fontFamilyChoices = new String[count];
                for( int i = 0; i < count; i++ )
                        fontFamilyChoices[i] = familyNames[i];

                fontFamilyChoiceCount = count;
                fontFamilyList = familyIndex;

                fontStyleChoices = new String[4];
                fontStyleChoices[0] = "REGULAR";
                fontStyleChoices[1] = "BOLD";
                fontStyleChoices[2] = "ITALIC";
                fontStyleChoices[3] = "BOLD ITALIC";
        }

        static String getTTFProfileName( int profile )
        {
                if( profile == 0 ) return "sl";
                if( profile == 1 ) return "console";
                if( profile == 2 ) return "simple";
                return "slii17";
        }

        static void applyTTFProfile( int profile )
        {
                initFontFamilyArrays();
                if( profile < 0 || profile > 3 ) return;
                int family = Osd.getTTFProfileFamily( profile );
                int style = Osd.getTTFProfileStyle( profile );
                String base = "UI-stock";
                if( family > 0 && family <= fontFamilyIds.length ) base = fontFamilyIds[family-1];
                String suffix = "";
                if( style > 0 && style <= 3 ) suffix = fontFamilyStyleSuffix[style];
                String id = base + suffix;
                String source = "Fonts/families/" + id;
                if( !File.exists( source ) ) source = "Fonts/families/" + base;
                if( File.exists( source ) ) File.copy( source, "Fonts/TTF.profile." + getTTFProfileName(profile) );
        }

        void chooseTTFProfileFamily( int profile )
        {
                int pos = osd.multiValue;
                if( pos < 0 ) pos = 0;
                if( pos >= fontFamilyChoiceCount ) pos = fontFamilyChoiceCount - 1;
                Osd.setTTFProfileFamily( profile, fontFamilyList[pos] );
                applyTTFProfile( profile );
        }

        void chooseTTFProfileStyle( int profile )
        {
                Osd.setTTFProfileStyle( profile, osd.multiValue );
                applyTTFProfile( profile );
        }

        static int snapTTFResourceScale( int percent )
        {
                if( percent < 60 ) percent = 60;
                if( percent > 150 ) percent = 150;
                percent = ((percent + 2) / 5) * 5;
                if( percent < 60 ) percent = 60;
                if( percent > 150 ) percent = 150;
                return percent;
        }

        void setTTFResourceScale( int resource, int percent )
        {
                String[] names = new String[9];
                names[0] = "sl14"; names[1] = "sl28"; names[2] = "slii11"; names[3] = "slii24";
                names[4] = "console5"; names[5] = "console10"; names[6] = "simple20"; names[7] = "simple40"; names[8] = "slii17";
                if( resource < 0 || resource > 8 ) return;
                percent = snapTTFResourceScale( percent );
                Osd.setTTFResourceScale( resource, percent );
                File.copy( "Fonts/scale/" + Integer.toString(percent), "Fonts/TTF.scale." + names[resource] );
        }

        // Copies the plain pointer file Fonts/TTF.font naming the currently
        // selected family/style. The exe reads that file on startup and loads
        // Fonts/families/<id>.ttf itself. (v2 copied the TTF over Fonts/UI.ttf,
        // but GDI keeps UI.ttf locked for the whole process lifetime, so the
        // in-game copy silently failed and the setting never applied.)
        // Returns 1 when a pointer file was written, 0 when the choice fell
        // back to the stock font.
        static int applyFontFamilyStyle()
        {
                initFontFamilyArrays();

                int family = Osd.getInterfaceFontFamily();
                int style = Osd.getInterfaceFontStyle();

                String base;

                if( family > 0 && family <= fontFamilyIds.length && File.exists( "Fonts/families/" + fontFamilyIds[family-1] + ".ttf" ) )
                {
                        base = fontFamilyIds[ family - 1 ];
                }
                else
                {
                        if( family != 0 )
                        {
                                family = 0;
                                Osd.setInterfaceFontFamily( 0, fontFamilyChoiceCount );
                        }
                        base = "UI-stock";
                }

                String suffix = "";
                if( style > 0 && style <= 3 )
                        suffix = fontFamilyStyleSuffix[ style ];

                String id = base + suffix;
                String src = "Fonts/families/" + id;
                if( !File.exists( src ) )
                {
                        id = base;
                        src = "Fonts/families/" + id;
                }

                if( !File.exists( src ) )
                        return 0;

                // raw byte copy of the one-line pointer file (File.copy does
                // not wrap files into the SDAT container); the exe accepts
                // [a-z0-9-] ids and ignores anything else
                File.copy( src, "Fonts/TTF.font" );
                return 1;
        }

        // menu position <-> family id (ids shift when family files are missing)
        static int fontFamilyIdToPosition( int id )
        {
                if( fontFamilyList != null )
                        for( int i = 0; i < fontFamilyChoiceCount; i++ )
                                if( fontFamilyList[i] == id )
                                        return i;
                return 0;
        }

        static int[] fontFamilyList;

        public void setDetailText( Text txt, int detail )
        {
                if( detail == Config.OFF )
                        txt.changeText( "Off" );
                else
                if( detail == Config.LOW )
                        txt.changeText( "Low" );
                else
                if( detail == Config.MID )
                        txt.changeText( "Mid" );
                else
                if( detail == Config.HIGH )
                        txt.changeText( "High" );
                /*else
                if( detail == Config.SHIGH )
                        txt.changeText( "Super High" );*/
        }

        public void setEnabledText( Text txt, int enabled )
        {
                if( enabled == 0 )
                        txt.changeText( "Disabled" );
                else
                        txt.changeText( "Enabled" );
        }

        public int ChangeDetail (int value, int delta, int min, int max)
        {
                value += delta;
                if( value < min )
                        value = max;
                if( value > max )
                        value = min;
                return value;
        }

        public int fequal( float v1, float v2 )
        {
                float d = v1 - v2;
                if( d < 0 )
                        d = -d;
                if( d < 0.001 )
                        return 1;

                return 0;
        }

        public void osdCommand (int command)
        {
                if (command < 0)
                        return;
                else
                if (command == CMD_AXISCHECK)
                {
                        axisCheckCallerGroup = actGroup;
                        changeMode (axisCheckGroup);

                        //a redefine menuk -ahonnan mi elerhetoek vagyunk- mar beallitottak, de meg nem mentettek a beallitasokat
                        player.controller.setcontrol( controlSet );
                }
                else
                if (command == CMD_AXISCHECK_DONE)
                {
                        changeMode (axisCheckCallerGroup);
                }
                else
                if (command == CMD_EFFECTS_VOL)
                {
                        Sound.setVolume ( Sound.CHANNEL_EFFECTS, osd.sliderValue );
                }
                else
                if (command == CMD_MUSIC_VOL)
                {
                        Sound.setVolume ( Sound.CHANNEL_MUSIC, osd.sliderValue );
                }
                else
                if (command == CMD_ENGINE_VOL)
                {
                        Sound.setVolume ( Sound.CHANNEL_ENGINE, osd.sliderValue );
                }
                else
                if (command == CMD_OPTIONS)
                {
                        changeMode (optionsGroup);
                }
                else
                if (command == CMD_VIDEO_OPTIONS)
                {
                        changeMode (videoGroup);
                }
                else
                if (command == CMD_VIDEO_OPTIONS_DONE)
                {
                        System.getConfigOptions();

                        if(Config.video_x != aspectX || Config.video_y != aspectY || Config.video_depth != color_depth || Config.video_windowed != windowed )
                        {
                                int Correct_Mode = 0;

                                if(Config.video_windowed == 1)
                                {
                                        int d = ( 0 == new NoYesDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved." ).display() );

                                        Config.video_windowed = windowed;
                                        Config.video_x=aspectX;
                                        Config.video_y=aspectY;
                                        Config.video_depth=color_depth;

                                        if( d == 1 )
                                        {
                                                //if( carreerMode )
                                                        GameLogic.autoSaveQuiet(); // Save

                                                GameLogic.changeActiveSection( null ); // Exit game
                                        }
                                        else
                                        {
                                                changeMode(optionsGroup);
                                        }
                                }
                                else
                                {
                                        if(Config.video_windowed != windowed)
                                        {
                                                int d = ( 0 == new NoYesDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved." ).display() );

                                                Config.video_windowed = windowed;
                                                Config.video_x=aspectX;
                                                Config.video_y=aspectY;
                                                Config.video_depth=color_depth;

                                                if( d == 1 )
                                                {
                                                        //if( carreerMode )
                                                                GameLogic.autoSaveQuiet(); // Save

                                                        GameLogic.changeActiveSection( null ); // Exit game
                                                }
                                                else
                                                {
                                                        changeMode(optionsGroup);
                                                }
                                        }
                                        else
                                        {
                                                // check resolution
                                                for (int i=2; i<=ResX_list.length-1; i++ ) 
                                                {
                                                        if(aspectX == ResX_list[i] && aspectY == ResY_list[i]) // found corect mode
                                                        {
                                                                Correct_Mode = 1;
                                                                i += ResX_list.length; //end 'for' loop
                                                        }
                                                }

                                                if( Correct_Mode )
                                                {
                                                        Config.video_windowed=windowed;
                                                        Config.video_x=aspectX;
                                                        Config.video_y=aspectY;
                                                        Config.video_depth=color_depth;

                                                        GfxEngine.changeVideoMode( 320, 240, 16 ); // refresh for more compatible
                                                        GfxEngine.changeVideoMode( 640, 480, 32 ); // refresh for more compatible
                                                        GfxEngine.changeVideoMode( Config.video_x, Config.video_y, Config.video_depth );

                                                        Frontend.setFonts();

                                                        changeMode(optionsGroup);
                                                }
                                                else
                                                {
                                                        new WarningDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "This resolution can't be use at full screen mode! \n \n Please change resolution or video mode." ).display();
                                                }
                                        }
                                }
                        }
                        else
                        {
                                changeMode(optionsGroup);

                                if( tmp_lp != loading_primary )
                                {
                                        if( 0 == new NoYesDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Game restart is requiring to apply changes of Loading Primary. \n Do you want make this now? \n \n Note that your current career will be saved." ).display() )
                                        {
                                                GameLogic.autoSaveQuiet(); // Save
                                                GameLogic.changeActiveSection( null ); // Exit game
                                        }
                                }
                        }
                }
                else
                if (command == CMD_M_GROUP)
                {
                        changeMode(MirrorGroup);
                }
                if (command == CMD_RESOLUTION)
                {
                        if(Current_Resolution < ResX_list.length-1)
                                Current_Resolution += 1;
                        else
                                Current_Resolution = 0;

                        aspectX = ResX_list[Current_Resolution];
                        aspectY = ResY_list[Current_Resolution];

                        osdCommand(CMD_REFRESH_TEXT);
                }
                else
                if (command == CMD_REFRESH_TEXT)
                {
                        if(!aspectX)
                        {
                                aspectX = Config.video_x;
                                aspectY = Config.video_y;
                        }

                        int TMPasp = 10*aspectX/aspectY;
                        aspect = TMPasp;

                        if(aspect == 10)        Res_Aspect = "1:1"; else
                        if(aspect == 12)        Res_Aspect = "5:4"; else
                        if(aspect == 13)        Res_Aspect = "4:3"; else
                        if(aspect == 14)        Res_Aspect = "13:9"; else
                        if(aspect == 15)        Res_Aspect = "14:9"; else
                        if(aspect == 16)        Res_Aspect = "5:3"; else
                        if(aspect == 17)        Res_Aspect = "16:9"; else
                        if(aspect == 20)        Res_Aspect = "6:3"; else
                        if(aspect == 23)        Res_Aspect = "12:5"; else
                        if(aspect == 24)        Res_Aspect = "12:5"; else
                        Res_Aspect = "OTHER";

                        if(Resolution_button)
                        {
                                Resolution_button.changeLabelText( "SCREEN RESOLUTION: "+Float.toString(aspectX, "%.0f")+" X "+Float.toString(aspectY, "%.0f")+" ("+Res_Aspect+")");
                                Depth_button.changeLabelText("COLOR DEPTH: "+color_depth);
                                
                                if(windowed == 0)
                                        windowed_button.changeLabelText( "DISPLAY MODE: FULL SCREEN" );
                                else
                                        windowed_button.changeLabelText( "DISPLAY MODE: WINDOWED" );
                        }
                }
                else
                if(command == CMD_DEPTH)
                {
                        if(Config.video_depth == 16)
                                color_depth = 32;
                        else
                                color_depth = 16;
                                
                        Depth_button.changeLabelText("COLOR DEPTH: "+color_depth);
                }
                if(command == CMD_MODE)
                {
                        if(windowed == 0)
                        {
                                windowed = 1;
                                windowed_button.changeLabelText( "DISPLAY MODE: WINDOWED" );
                        }
                        else
                        {
                                windowed = 0;
                                windowed_button.changeLabelText( "DISPLAY MODE: FULL SCREEN" );
                        }
                }
                if(command == CMD_UNRES)
                {
                        new UR_Dialog().display();
                        osdCommand(CMD_REFRESH_TEXT);
                }
                if (command == CMD_TEXTURE_DETAIL)
                {
                        textureDetail = osd.multiValue;
                        Config.texture_size = 4-textureDetail;
                }
                else
                if (command == CMD_ABS_SLIDER)
                {
                        Config.player_abs = osd.sliderValue;
                }
                else
                if (command == CMD_ASR_SLIDER)
                {
                        Config.player_asr = osd.sliderValue;
                }
                else
                if (command == CMD_HMF_1)
                {
                        Config.head_move_steer = osd.sliderValue;
                }
                else
                if (command == CMD_HMF_2)
                {
                        Config.head_move_vel = osd.sliderValue;
                }
                else
                if (command == CMD_HMF_3)
                {
                        Config.head_move_acc = osd.sliderValue;
                }
                else
                if (command == CMD_TTF_SCALE)
                {
                        setTTFScalePercent( (int)(osd.sliderValue + 0.5f) );
                }
                else
                if (command == CMD_TTF_FAMILY) chooseTTFProfileFamily( 0 );
                if (command == CMD_TTF_STYLE) chooseTTFProfileStyle( 0 );
                if (command == CMD_TTF_PROFILE_FAMILY_CONSOLE) chooseTTFProfileFamily( 1 );
                if (command == CMD_TTF_PROFILE_STYLE_CONSOLE) chooseTTFProfileStyle( 1 );
                if (command == CMD_TTF_PROFILE_FAMILY_SIMPLE) chooseTTFProfileFamily( 2 );
                if (command == CMD_TTF_PROFILE_STYLE_SIMPLE) chooseTTFProfileStyle( 2 );
                if (command == CMD_TTF_PROFILE_FAMILY_SLII17) chooseTTFProfileFamily( 3 );
                if (command == CMD_TTF_PROFILE_STYLE_SLII17) chooseTTFProfileStyle( 3 );
                if (command == CMD_TTF_SCALE_SL14) setTTFResourceScale( 0, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SL28) setTTFResourceScale( 1, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SLII11) setTTFResourceScale( 2, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SLII24) setTTFResourceScale( 3, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_CONSOLE5) setTTFResourceScale( 4, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_CONSOLE10) setTTFResourceScale( 5, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SIMPLE20) setTTFResourceScale( 6, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SIMPLE40) setTTFResourceScale( 7, (int)(osd.sliderValue + 0.5f) );
                if (command == CMD_TTF_SCALE_SLII17) setTTFResourceScale( 8, (int)(osd.sliderValue + 0.5f) );
                else
                if (command == CMD_SHADOW_DETAIL)
                {
                        shadow_size = osd.multiValue;

                        if( shadow_size == 7 )
                        {
                                Config.shadow_size = 8192;
                                Config.shadows = 2;
                        }
                        else
                        if( shadow_size == 6 )
                        {
                                Config.shadow_size = 4096;
                                Config.shadows = 2;
                        }
                        else
                        if( shadow_size == 5 )
                        {
                                Config.shadow_size = 2048;
                                Config.shadows = 2;
                        }
                        else
                        if( shadow_size == 4 )
                        {
                                Config.shadow_size = 1024;
                                Config.shadows = 2;
                        }
                        else
                        if( shadow_size == 3 )
                        {
                                Config.shadow_size = 512;
                                Config.shadows = 1;
                        }
                        else
                        if( shadow_size == 2 )
                        {
                                Config.shadow_size = 256;
                                Config.shadows = 1;
                        }
                        else
                        if( shadow_size == 1 )
                        {
                                Config.shadow_size = 128;
                                Config.shadows = 1;
                        }
                        else
                        {
                                Config.shadow_size = 32;
                                Config.shadows = 0;
                        }
                        //CMD_VIDEO_OPTIONS_DONE hatasara lep ervenybe!
                        refreshCameras();
                }
                else
                if (command == CMD_SHADOW_RANGE)
                {
                        Config.shadow_detail = 1/osd.sliderValue;
                        Config.shadow_detail = Config.shadow_detail*10;
                        ShadowDist_Text.changeLabelText("SHADOW VIEW DISTANCE: " + Float.toString(1/Config.shadow_detail*10, "%.0f m") );

                        refreshCameras();
                }
                else
                if(command == CMD_LP)
                {
                        loading_primary = osd.multiValue;

                        if(loading_primary == 0) // LOW
                        {
                                Config.resource_refreshrate = 512;
                                Config.resource_loadrate = 512;
                        }
                        else
                        if(loading_primary == 1) // MEDIUM
                        {
                                Config.resource_refreshrate = 2048;
                                Config.resource_loadrate = 2048;
                        }
                        if(loading_primary == 2) // HIGHT
                        {
                                Config.resource_refreshrate = 12288;
                                Config.resource_loadrate = 12288;
                        }
                }
                if (command == CMD_FLARES)
                {
                        Config.flares = osd.multiValue;
                        refreshCameras();
                }
                else
                if (command == CMD_HEADLIGHTS)
                {
                        Config.headlight_rays = osd.multiValue;
                        refreshCameras();
                }
                /*else
                if (command == CMD_BGSCENE)
                {
                        Config.BgScene = osd.multiValue;
                }
                else
                if (command == CMD_MIRROR)
                {
                        Config.mirror = osd.multiValue;
                }*/
                else
                if (command == CMD_VIEW_RANGE)
                {
                        Config.cameras_viewrange = osd.sliderValue;
                        Config.camera_ext_viewrange = osd.sliderValue;
                        Config.camera_int_viewrange = osd.sliderValue/2;
                        Dist_Text.changeLabelText("VIEW DISTANCE: " + Float.toString(Config.cameras_viewrange, "%.0f m"));
                        refreshCameras();
                }
                /*else
                if (command == CMD_MIRROR_DISTANCE)
                {
                        Config.camera_mirror_viewrange = osd.sliderValue;
                        MirrorDist_Text.changeLabelText("MIRROR VIEW DISTANCE: " + Float.toString(Config.camera_mirror_viewrange, "%.0f m"));
                        refreshCameras();
                }*/
                else
                if (command == CMD_OBJECT_DETAIL)
                {
                        Config.object_detail = osd.sliderValue;
                        refreshCameras();
                }
                else
                if (command == CMD_LOD_DETAIL)
                {
                        Config.object_detail_amp = osd.sliderValue;
                        refreshCameras();
                }
                else
                if (command == CMD_PARTICLE)
                {
                        Config.particle_density = osd.sliderValue;
                        Particle_Text.changeLabelText("PARTICLE DENSITY: " + Float.toString(Config.particle_density*100, "%.0f")+"%");
                        refreshCameras();
                }
                else
                if (command == CMD_GAMMA)
                {
                        Config.video_gamma = osd.sliderValue;
                        Gamma_Text.changeLabelText("GAMMA: " + Float.toString(Config.video_gamma, "%.2f"));
                        refreshCameras();
                }
                else
                if (command == CMD_CONTROL_OPTIONS)
                {
                        changeMode (controlGroup);
                }
                else
                if (command == CMD_GAME_OPTIONS)
                {
                        changeMode (gameGroup);
                }
                else
                if (command == CMD_GAME_OPTIONS2)
                {
                        changeMode (game2Group);
                }
                else
                if (command == CMD_INTERFACE)
                {
                        changeMode (interfaceGroup);
                }
                else
                if (command == CMD_FONT_OPTIONS)
                {
                        changeMode (fontGroup);
                }
                else
                if (command == CMD_GPSMODE)
                {
                        Config.gpsMode = osd.multiValue;
                }
                if (command == CMD_METRIC)
                {
                        Config.metricSystem = osd.multiValue;           // miles, kilometers, 
                        if( Config.metricSystem == 0 )
                                System.setMeasure( 1600 );
                        else
                                System.setMeasure( 1000 );
                }
                else
                if (command == CMD_DIFFICULTY)
                {
                        Config.deformation = osd.sliderValue;
                        Config.internal_damage = osd.sliderValue;
                        Config.player_damage_multiplier = osd.sliderValue / 2.0;
                }
                else
                if (command == CMD_TRAFFICDENSITY)
                {
                        Config.trafficDensity = osd.sliderValue;
                }
                else
                if (command == CMD_PEDESTRIANDENSITY)
                {
                        Config.pedestrianDensity = osd.sliderValue;
                }
                if (command == CMD_SKIDMARK)
                {
                        Config.skidmark_max = osd.sliderValue * 4.0;
                        SkidMark_Text.changeLabelText("SKIDMARKS LENGTH: "+Config.skidmark_max/4+" m");
                }
                else
                if (command == CMD_CLUTCH)
                {
                        Config.player_transmission = clutchTrfInv[osd.multiValue];
                }
                else
                if (command == CMD_STEERHELP)
                {
                        Config.player_steeringhelp = osd.sliderValue;
                }
                else
                if (command == CMD_ARCADE)
                {
                        drift_mode = osd.multiValue;

                        if( drift_mode == 0 ) // OFF
                        {
                                Config.ground_friction = 1.0;
                        }
                        else
                        if( drift_mode == 1 ) // MEDIUM
                        {
                                Config.ground_friction = 0.8;
                        }
                        else
                        if( drift_mode == 2 ) // HIGH
                        {
                                Config.ground_friction = 0.6;
                        }
                        else
                        if( drift_mode == 3 ) // POWERSLIDE
                        {
                                Config.ground_friction = 1.4;
                        }
                }
                else
                if (command == CMD_SWAYBARMODE)
                {
                        Config.swaybarsMode = osd.multiValue;
                }
                else
                if (command == CMD_DRIVERMODELS)
                {
                        Config.driverModel = osd.multiValue;
                        refreshCameras();
                }
                else
                if (command == CMD_MAPFOG)
                {
                        Config.mapFog = osd.multiValue;
                        refreshCameras();
                }
                else
                if (command == CMD_DAYRACERS)
                {
                        Config.dayRacers = osd.multiValue;
                }
                else
                if (command == CMD_NIGHTRACERS)
                {
                        Config.nightRacers = osd.multiValue;
                }
                else
                if (command == CMD_TUNEABLEDOORS)
                {
                        Config.tuneableDoors = osd.multiValue;
                }
                else
                if (command == CMD_CATALOGSTYLE)
                {
                        Config.catalogStyle = osd.multiValue;
                }
                else
                if (command == CMD_CARCATCOLOR)
                {
                        Config.carCatalogColor = osd.multiValue;
                }
                else
                if (command == CMD_INV_AUTOSCROLL)
                {
                        Config.invAutoScroll = osd.multiValue;
                }
                else
                if (command == CMD_INV_GRIDSIZE)
                {
                        Config.invGridSize = osd.multiValue;
                }
                else
                if (command == CMD_TTF_COLOR)
                {
                        Osd.setInterfaceFontColorMode( osd.multiValue );
                }
                else
                if (command == CMD_GAME_OPTIONS_DONE)
                {
                        System.getConfigOptions();
                        {
                                changeMode( optionsGroup );

                                if( tmp_drft != drift_mode || tmp_swaybars != Config.swaybarsMode )
                                {
                                        tmp_drft = drift_mode; tmp_swaybars = Config.swaybarsMode;
                                        if( 0 == new NoYesDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved." ).display() )
                                        {
                                                GameLogic.autoSaveQuiet(); // Save
                                                GameLogic.changeActiveSection( null ); // Exit game
                                        }
                                }
                                else if( this instanceof IngameMenu )
                                {
                                        if( tmp_fog != Config.mapFog || tmp_driver != Config.driverModel || ( tmp_dayr != Config.dayRacers || tmp_nightr != Config.nightRacers ) && this.mode == this.MM_CITY )
                                        {
                                                tmp_fog = Config.mapFog; tmp_dayr = Config.dayRacers; tmp_nightr = Config.nightRacers;
                                                new WarningDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, "WARNING", "Track restart is requiring to apply some changes. \n You can make it at any time you want. \n \n " ).display();
                                        }
                                }
                                if( tmp_driver != Config.driverModel )
                                {
                                        tmp_driver = Config.driverModel;
                                        GameLogic.updateDriverModels();
                                }
                        }
                }
                else
                if (command == CMD_INTERFACE_DONE)
                {
                        System.getConfigOptions();
                        {
                                changeMode( optionsGroup );

                                if( tmp_doors != Config.tuneableDoors )
                                {
                                        tmp_doors = Config.tuneableDoors;
                                        if( 0 == new NoYesDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "WARNING", "Game restart is requiring to apply changes. \n Do you want make this now? \n \n Note that your current career will be saved." ).display() )
                                        {
                                                GameLogic.autoSaveQuiet(); // Save
                                                GameLogic.changeActiveSection( null ); // Exit game
                                        }
                                }
                                else if( this instanceof IngameMenu )
                                {
                                        if( tmp_gridsize != Config.invGridSize )
                                        {
                                                tmp_gridsize = Config.invGridSize;
                                                new WarningDialog( GameLogic.player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.DF_WIDE, "WARNING", "Track restart is requiring to apply some changes. \n You can make it at any time you want. \n \n " ).display();
                                        }
                                }
                        }
                }
                else
                if (command == CMD_FONT_SCALE_OPTIONS_DONE)
                {
                        changeMode( fontGroup );
                }
                else
                if (command == CMD_FONT_OPTIONS_DONE)
                {
                        changeMode( optionsGroup );
                }
                else
                if (command == CMD_SOUND_OPTIONS)
                {
                        //lehetseges modeok, es kijelzesuk
                        //MIX   OFF     ON      ON      AUTO
                        //3D    OFF     OFF     ON      AUTO
                        //              OFF     2D      3D      AUTO

                        int soundHW = Config.Sound_Mix_HW;
                        if( soundHW == 2 )
                                soundHW++;
                        if( Config.Sound_Mix_HW == 1 && Config.Sound_3D_HW == 1 )
                                soundHW++;

                        soundHWMulti.setValue( soundHW );

                        changeMode (soundGroup);
                }
                else
                if (command == CMD_SOUND_HW)
                {
                        switch( osd.multiValue )
                        {
                        case 0:
                                Config.Sound_Mix_HW = 0;
                                Config.Sound_3D_HW = 0;
                                break;
                        case 1:
                                Config.Sound_Mix_HW = 1;
                                Config.Sound_3D_HW = 0;
                                break;
                        case 2:
                                Config.Sound_Mix_HW = 1;
                                Config.Sound_3D_HW = 1;
                                break;
                        case 3:
                                Config.Sound_Mix_HW = 2;
                                Config.Sound_3D_HW = 2;
                                break;
                        }
                        //TODO: tell the sound engine to apply the changes
                }
                else
                if (command == CMD_MAIN)
                {
                        changeMode (mainGroup);
                }
                else
                if (command == CMD_RED_SUSP || command == CMD_REDEFINE_CONTROLS || command == CMD_REDEFINE_CONTROLS2 || command == CMD_REDEFINE_CONTROLS3)
                {
                        int     i;

                        controlSet = new ControlSet();
                        controlSet.load( GameLogic.activeControlFile );

                        for( int i = 0; i < NCONTROLS; i++ )
                        {
                                if( keyText[i] )
                                {
                                        if (controlSet.deviceID[i] < 0 || controlSet.axisID[i] < 0)
                                                keyText[i].changeText( "Undefined" );
                                        else
                                                keyText[i].changeText( Input.axisName (controlSet.deviceID[i], controlSet.axisID[i]) );
                                }
                        }

                        powerSlider[0].setValue( (controlSet.vasp.elementAt(0).power - 1.0) / 4.0 );    // value: 1.0..5.0
                        powerSlider[1].setValue( (controlSet.vasp.elementAt(1).power - 1.0) / 4.0 );
                        powerSlider[2].setValue( (controlSet.vasp.elementAt(2).power - 1.0) / 4.0 );
                        powerSlider[3].setValue( (controlSet.vasp.elementAt(3).power - 1.0) / 4.0 );

                        dzSlider[0].setValue( controlSet.dead_zone[0] / 0.2 );                                                  // value: 0.0..0.2
                        dzSlider[1].setValue( controlSet.dead_zone[1] / 0.2 );
                        dzSlider[2].setValue( controlSet.dead_zone[2] / 0.2 );
                        dzSlider[3].setValue( controlSet.dead_zone[7] / 0.2 );

                        if(command == CMD_REDEFINE_CONTROLS)
                                changeMode (redefineGroup);
                        else 
                        if(command == CMD_REDEFINE_CONTROLS2)
                                changeMode (redefine2Group);
                        else 
                        if(command == CMD_RED_SUSP)
                                changeMode (redefineSuspGroup);
                        else
                                changeMode (redefine3Group);
                }
                else
                if (command == CMD_FIXED_CONTROLS)
                {
                        changeMode (fixedCtrlsGroup);
                }
                else
                if (command == CMD_FIXED_CONTROLS_DONE)
                {
                        changeMode (controlGroup);
                }
                else
                if (command == CMD_REDEFINE_DONE)
                {
                        controlSet.save( GameLogic.activeControlFile );
                        changeMode (controlGroup);
                }
                else
                if (command == CMD_RESET_CONTROLS)
                {
                        controlSet = new ControlSet();
                        controlSet.save( GameLogic.activeControlFile );
                        player.controller.setcontrol( controlSet );
                }
                else
                if (command == CMD_LOAD_CONTROLS)
                {
                        ControlsFileReqDialog d = new ControlsFileReqDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG, "LOAD CONTROL DEFINITIONS", "LOAD CONTROLS", GameLogic.controlSaveDir, "*" );

                        if( d.display() == 0 )
                        {
                                String filename = GameLogic.controlSaveDir + d.fileName;

                                controlSet = new ControlSet();
                                controlSet.load( filename );
                                controlSet.save( GameLogic.activeControlFile );
                                player.controller.setcontrol( controlSet );
                        }
                }
                else
                if (command == CMD_SAVE_CONTROLS)
                {
                        ControlsFileReqDialog d = new ControlsFileReqDialog( player.controller, Dialog.DF_MODAL|Dialog.DF_DEFAULTBG|Dialog.FRF_SAVE, "SAVE CONTROL DEFINITIONS", "SAVE CONTROLS", GameLogic.controlSaveDir, "*" );

                        if( d.display() == 0 )
                        {
                                String filename = GameLogic.controlSaveDir + d.fileName;

                                if( File.exists( GameLogic.activeControlFile ) )
                                {
                                        File.copy( GameLogic.activeControlFile, filename );
                                }
                                else
                                {       //elso inditas, meg nem letezik a file!!
                                        controlSet = new ControlSet();
                                        controlSet.save( filename );
                                }
                        }
                }
                else
                if (command >= CMD_GETKEY && command < CMD_GETKEY + NCONTROLS)
                {
                        if (getKey == -1)
                        {
                                getKey = command - CMD_GETKEY;
                                keyText[getKey].changeText("_");
                                getKeyStat = 0;
                                // special request: reset mouse and set sensitivity
                                Input.getAxis (1, -1 - (Config.mouseSensitivity * 100.0f));
                        }
                }
                else
                if (command >= CMD_DEAD_ZONE && command < CMD_DEAD_ZONE + 4)
                {
                        int             dz;

                        dz = command - CMD_DEAD_ZONE;   // remap 0,1,2,3 -> 0,1,2,7
                        if (dz == 3)
                        {
                                dz = 7;
                        }
                        controlSet.dead_zone[dz] = osd.sliderValue * 0.2f;
                        controlSet.dead_zone[dz + 15] = osd.sliderValue * 0.2f;
                        player.controller.setcontrol( controlSet );
                }
                else
                if (command >= CMD_POWER && command < CMD_POWER + 4)
                {
                        float pow = 1.125 + 4.0 * osd.sliderValue;
                        int ipower = pow * 4;
                        pow = ipower;
                        pow = pow / 4.0;
                        controlSet.vasp.elementAt( command - CMD_POWER ).power = pow;
                        player.controller.setcontrol( controlSet );
                }
                else
                if (command == CMD_FFB + 0)
                {
                        Config.FFB_strength = osd.sliderValue;
                        System.getConfigOptions();
                } 
                else
                if (command == CMD_FFB + 1)
                {
                        Config.FFB_strength_emulated = osd.sliderValue;
                        System.getConfigOptions();
                } 
                else
                if (command == CMD_MOUSE_SENS)
                {
                        Config.mouseSensitivity = osd.sliderValue;
                        if (Config.mouseSensitivity < 0.1)
                                Config.mouseSensitivity = 0.1;
                        // special request: reset mouse and set sensitivity
                        Input.getAxis (1, -1 - (Config.mouseSensitivity * 100.0f));
                        //send command to cursor
                        Input.cursor.config();
                }
        }

        public void changeMode(int group)
        {
                if( actGroup != group )
                {
                        if (actGroup >= 0)
                        {
                                osd.hideGroup (actGroup);
                        }

                        actGroup = group;

                        osd.showGroup (actGroup);

                        osd.changeSelection2( -1, 0 );

                        if(actGroup == videoGroup)
                                osdCommand(CMD_REFRESH_TEXT);
                }
        }

        public void animate()
        {       
                //control remapper
                if (actGroup == redefineGroup || actGroup == redefine2Group || actGroup == redefineSuspGroup)
                {
                        if (getKey >= 0 && getKey < NCONTROLS)
                        {
                                int             device;
                                int             axis = -1;

                                for( device = 0; device < controlSet.nDevices; device++ )
                                {
                                        axis = Input.activeAxis (device);
                                        if( axis >= 0 )
                                                break;
                                }

                                if (getKeyStat == 0) 
                                {
                                        if (axis < 0) 
                                        {
                                                getKeyStat = 1;
                                        }
                                } 
                                else 
                                if (getKeyStat == 1)
                                {
                                        if( device == 0 && axis == Input.RCDIK_ESCAPE )
                                        {
                                                keyText[getKey].changeText( "Undefined" );
                                                controlSet.change( getKey, -1, -1 );
                                                player.controller.setcontrol( controlSet );
                                                getKeyStat = 2;
                                        }
                                        else
                                        {
                                                if (axis >= 0) 
                                                {
                                                        float val = Input.getAxis (device, axis);
                                                        keyText[getKey].changeText (Input.axisName (device, axis));
                                                        controlSet.change( getKey, device, axis);
                                                        int i;
                                                        for (i = 0; i < NCONTROLS; i++) 
                                                        {
                                                                if (i != getKey && controlSet.deviceID [i] == device && controlSet.axisID [i] == axis)
                                                                        break;
                                                        }
                                                        if (device == 0 || val > 0.95f) 
                                                        {
                                                                controlSet.from_min [getKey] = 0.0f;
                                                                controlSet.from_max [getKey] = 1.0f;
                                                        } 
                                                        else 
                                                        {
                                                                if (i < NCONTROLS) 
                                                                {
                                                                        if (val < 0.0f) 
                                                                        {
                                                                                controlSet.from_min [getKey] = 0.0f;
                                                                                controlSet.from_max [getKey] = -1.0f;
                                                                                controlSet.from_min [i] = 0.0f;
                                                                                controlSet.from_max [i] = 1.0f;
                                                                        } 
                                                                        else 
                                                                        {
                                                                                controlSet.from_min [getKey] = 0.0f;
                                                                                controlSet.from_max [getKey] = 1.0f;
                                                                                controlSet.from_min [i] = 0.0f;
                                                                                controlSet.from_max [i] = -1.0f;
                                                                        }
                                                                } 
                                                                else 
                                                                {
                                                                        controlSet.from_min [getKey] = -1.0f;
                                                                        controlSet.from_max [getKey] = 1.0f;
                                                                }
                                                        }
                                                        player.controller.setcontrol( controlSet );
                                                        getKeyStat = 2;
                                                }
                                        }
                                } 
                                else 
                                {
                                        if (axis < 0) 
                                        {
                                                getKey = -1;
                                        }
                                }
                        }
                }

                //gas/brake/steer axis position updater
                if (actGroup == axisCheckGroup)
                {
                        acGasSlider.setValue( Input.getInput( Input.AXIS_THROTTLE ) / 128.0 );
                        acBreakSlider.setValue( Input.getInput( Input.AXIS_BRAKE ) / 128.0 );
                        acSteerSlider.setValue( Input.getInput( Input.AXIS_TURN_LEFTRIGHT ) / -256.0 + 0.5);
                        acClutchSlider.setValue( Input.getInput( Input.AXIS_CLUTCH ) / 128.0);
                }
                super.animate();
        }

        GameRef activeCamera;

        public void refreshCameras()
        {
                System.getConfigOptions();

                //mega patchek:
                //( a fenti hivasnak kellene triggerelni minden aktiv 3d-engine kamerat, nem itt vacakolni veluk!!!)
                if( activeCamera )
                        activeCamera.command( "render 0" );

                if (GameLogic.player && GameLogic.player.car )
                        GameLogic.player.car.command( "render 0 "+ GameLogic.player.controller.id() );

        }

        public void setActiveCamera( GameRef cam )
        {
                activeCamera = cam;
        }

}


class ControlsFileReqDialog extends FileRequesterDialog
{
        public ControlsFileReqDialog( Controller ctrl, int myflags, String mytitle, String OKButtonText, String path, String mask )
        {
                super( ctrl, myflags, mytitle, OKButtonText, path, mask );
                if( myflags & FRF_SAVE )
                        osd.defSelection = 5;
        }

        public int validator( String filename )
        {
                return ControlSet.fileCheck( filename );
        }
}

class UR_Dialog extends Dialog
{
        final static int        CMD_BACK = 15;
        final static int        CMD_OK   = 16;

        final static int        CMD_X_UP = 20;
        final static int        CMD_X_DOWN = 21;

        final static int        CMD_Y_UP = 22;
        final static int        CMD_Y_DOWN = 23;

        Menu    m;
        Style   Menu_Center, Menu_Button;
        Text    ResText, AspectText;
        String  Res_Aspect;

        int[]   Res_list;
        int             SizeX, SizeY, numberX, numberY;
        float   aspect;

        public UR_Dialog()
        {
                super( GameLogic.player.controller, DF_MODAL|DF_DEFAULTBG|DF_HIGHPRI|DF_DARKEN, "UNCOMMON RESOLUTION", "" );

                Res_list = new int[24];
                Res_list[0]             = 600;
                Res_list[1]             = 648;
                Res_list[2]             = 720;
                Res_list[3]             = 768;
                Res_list[4]             = 800;
                Res_list[5]             = 864;
                Res_list[6]             = 900;
                Res_list[7]             = 960;
                Res_list[8]             = 1000;
                Res_list[9]             = 1024;
                Res_list[10]    = 1050;
                Res_list[11]    = 1080;
                Res_list[12]    = 1152;
                Res_list[13]    = 1200;
                Res_list[14]    = 1280;
                Res_list[15]    = 1360;
                Res_list[16]    = 1366;
                Res_list[17]    = 1400;
                Res_list[18]    = 1440;
                Res_list[19]    = 1600;
                Res_list[20]    = 1680;
                Res_list[21]    = 1776;
                Res_list[22]    = 1920;
                Res_list[23]    = 2560;

                for (int i=0; i<=Res_list.length-1; i++ ) // Get X Res
                {
                        if(OptionsDialog.aspectX == Res_list[i])
                        {
                                numberX = i;
                                i += Res_list.length; //end 'for' loop
                        }
                }

                for (int i=0; i<=Res_list.length-1; i++ ) // Get Y Res
                {
                        if(OptionsDialog.aspectY == Res_list[i])
                        {
                                numberY = i;
                                i += Res_list.length; //end 'for' loop
                        }
                }
                
                Menu_Center = new Style( 0.50, 0.13, Frontend.mediumFont, Text.ALIGN_CENTER, Osd.RRT_TEST );
                Menu_Button = new Style( 0.12, 0.12, Frontend.mediumFont, Text.ALIGN_CENTER, null );

                ResText = osd.createText( "----   X   ----" , Frontend.largeFont, Text.ALIGN_CENTER, 0.0, -0.3 ); ResText.changeColor(0xFFC0C0C0);// "Unselected" gray

                m = osd.createMenu( Menu_Button, -0.5, -0.5, 0, Osd.MD_VERTICAL );
                m.addItem( new ResourceRef( frontend:0x004Cr ), CMD_X_UP, "Increase X size" );

                m = osd.createMenu( Menu_Button, -0.5, 0.0, 0, Osd.MD_VERTICAL );
                m.addItem( new ResourceRef( frontend:0x0084r ), CMD_X_DOWN, "Decrease X size" );

                m = osd.createMenu( Menu_Button, 0.5, -0.5, 0, Osd.MD_VERTICAL );
                m.addItem( new ResourceRef( frontend:0x004Cr ), CMD_Y_UP, "Increase Y size" );

                m = osd.createMenu( Menu_Button, 0.5, 0.0, 0, Osd.MD_VERTICAL );
                m.addItem( new ResourceRef( frontend:0x0084r ), CMD_Y_DOWN, "Decrease Y size" );

                AspectText = osd.createText( "Aspect: N/A" , Frontend.largeFont, Text.ALIGN_CENTER, 0.0, 0.4 ); AspectText.changeColor(0xFFC0C0C0);// "Unselected" gray

                m = osd.createMenu( Menu_Center, -0.4, 0.9, 0, Osd.MD_HORIZONTAL );
                m.addItem( "OK", CMD_OK );
                
                m = osd.createMenu( Menu_Center, 0.4, 0.95, 0, Osd.MD_HORIZONTAL );
                m.addItem( "BACK", CMD_BACK );

                Refresh();

                super.show();
        }

        public void Refresh()
        {
                float x = Res_list[numberX];
                float y = Res_list[numberY];

                aspect = x/y;
                int TMPasp = 10*aspect;

                if(TMPasp == 10)        Res_Aspect = "1:1"; else
                if(TMPasp == 12)        Res_Aspect = "5:4"; else
                if(TMPasp == 13)        Res_Aspect = "4:3"; else
                if(TMPasp == 14)        Res_Aspect = "13:9"; else
                if(TMPasp == 15)        Res_Aspect = "14:9"; else
                if(TMPasp == 16)        Res_Aspect = "5:3"; else
                if(TMPasp == 17)        Res_Aspect = "16:9"; else
                if(TMPasp == 20)        Res_Aspect = "6:3"; else
                if(TMPasp == 23)        Res_Aspect = "12:5"; else
                if(TMPasp == 24)        Res_Aspect = "12:5"; else
                Res_Aspect = "OTHER";

                ResText.changeText( Res_list[numberX] + "   X   " + Res_list[numberY] );
                AspectText.changeText( "Aspect: " + aspect + " (" + Res_Aspect + ")" );
        }
                
        public void osdCommand( int cmd )
        {
                if( cmd == CMD_X_DOWN )
                {
                        if( numberX > 0)
                                numberX -= 1;
                        else
                                numberX = Res_list.length-1;

                        Refresh();
                }
                else
                if( cmd == CMD_X_UP )
                {
                        if( numberX < Res_list.length-1)
                                numberX += 1;
                        else
                                numberX = 0;

                        Refresh();
                }
                else
                if( cmd == CMD_Y_DOWN )
                {
                        if( numberY > 0)
                                numberY -= 1;
                        else
                                numberY = Res_list.length-1;

                        Refresh();
                }
                else
                if( cmd == CMD_Y_UP )
                {
                        if( numberY < Res_list.length-1)
                                numberY += 1;
                        else
                                numberY = 0;

                        Refresh();
                }
                else
                if( cmd == CMD_BACK )
                {
                        osd.hide();
                        super.hide();
                }
                else
                if( cmd == CMD_OK )
                {
                        OptionsDialog.aspectX = Res_list[numberX];
                        OptionsDialog.aspectY = Res_list[numberY];

                        osd.hide();
                        super.hide();
                }
        }
}
