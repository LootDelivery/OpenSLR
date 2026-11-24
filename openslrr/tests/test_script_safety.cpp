#include "save_io_fixture.hpp"
#include "slrr/script_compiler.hpp"
#include "slrr/host_game_options.hpp"
#include "slrr/gameref.hpp"
#include "slrr/ui_time.hpp"
#include <limits>
using namespace slrr;
int main() {
  std::setvbuf(stdout,nullptr,_IONBF,0);
  Fixture f;
  auto I=[](int32_t n){return Value::make_int(n);};
  auto F=[](float n){return Value::make_float(n);};
  Object object;
  const auto nan=F(std::numeric_limits<float>::quiet_NaN());
  SLRR_REQUIRE(native_arg_integer({},0,7)==7);
  SLRR_REQUIRE(native_arg_integer({Value::make_obj(&object)},0,7)==7);
  SLRR_REQUIRE(native_arg_integer({nan},0,7)==7);
  SLRR_REQUIRE(native_arg_integer({F(3.75f)},0)==3);
  SLRR_REQUIRE(native_arg_integer({F(1e30f)},0)==INT32_MAX);
  SLRR_REQUIRE(native_arg_object({I(1)},0)==nullptr);
  register_script_natives(f.vm,f.packs,f.sections);
  register_osd_natives(f.vm,f.packs,f.sections);
  register_lang_natives(f.vm,f.packs,f.sections);
  register_renderref_natives(f.vm,f.packs,f.sections);
  auto call=[&](const char* owner,const char* method,const char* sig,std::vector<Value> args,Object* self=nullptr){
    return f.registry.invoke(owner,method,sig,self,args);
  };
  SLRR_REQUIRE(call("java.game.OptionsDialog","getRendererOption","(I)I",{}).v.i==0);
  auto& graphics=host_enhanced_graphics();const int previous=graphics.msaa;
  call("java.game.OptionsDialog","setRendererOption","(II)V",{});
  SLRR_REQUIRE(graphics.msaa==previous);
  call("java.game.OptionsDialog","setRendererOption","(II)V",{F(0),F(2)});
  SLRR_REQUIRE(graphics.msaa==2);graphics.msaa=previous;
  // The GAME OPTIONS handbrake row: the shipped dialog class asks these two.
  const float handbrake = call("java.game.OptionsDialog","getHandbrakeStrength","()F",{}).v.f;
  SLRR_REQUIRE(handbrake > 0.f);
  call("java.game.OptionsDialog","setHandbrakeStrength","(F)V",{F(2.5f)});
  SLRR_REQUIRE(call("java.game.OptionsDialog","getHandbrakeStrength","()F",{}).v.f==2.5f);
  call("java.game.OptionsDialog","setHandbrakeStrength","(F)V",{F(handbrake)});
  SLRR_REQUIRE(call("java.game.OptionsDialog","getHandbrakeStrength","()F",{}).v.f==handbrake);
  call("java.game.OptionsDialog","setHandbrakeStrength","(F)V",{F(99.f)});
  SLRR_REQUIRE(call("java.game.OptionsDialog","getHandbrakeStrength","()F",{}).v.f==5.f);
  call("java.game.OptionsDialog","setHandbrakeStrength","(F)V",{F(handbrake)});
  for (auto args : {std::vector<Value>{},std::vector<Value>{I(1)},std::vector<Value>{Value::make_obj(&object),nan,I(32)}})
    call("java.render.GfxEngine","changeVideoMode","(III)V",args);
  call("java.game.OptionsDialog","setRendererOption","(II)V",{I(0),Value::make_obj(&object)});
  SLRR_REQUIRE(graphics.msaa==previous);
  Object text,font;
  slrr_text_create(&text,&font,0,0);slrr_text_set_string(&text,"test");
  slrr_text_update(&text);
  call("java.render.Text","setPos","(FF)V",{I(1),F(.5f)},&text);
  SLRR_REQUIRE(slrr_osd_texts().back().x==1.f && slrr_osd_texts().back().y==.5f);
  slrr_text_destroy(&text);
  Object render,position,orientation;
  gameref_upsert(&render,GameRefKind::RenderRef);
  position.fields["x"]=I(2);position.fields["y"]=F(3);position.fields["z"]=I(4);
  orientation.fields["y"]=I(1);
  call("java.util.resource.RenderRef","setMatrix","(Ljava.lang.Vector3;Ljava.lang.Ypr;)V",
       {Value::make_obj(&position),Value::make_obj(&orientation)},&render);
  const auto* state=gameref_get(&render);
  SLRR_REQUIRE(state && state->pos[0]==2 && state->pos[1]==3 && state->pos[2]==4);
  gameref_remove(&render);
  Object camera;
  call("java.render.Camera","create","(Ljava.util.resource.ResourceRef;Ljava.render.Viewport;IFFFFFII)V",{Value::make_obj(nullptr),Value::make_obj(nullptr),F(3)},&camera);
  SLRR_REQUIRE(gameref_get(&camera)->cam_pri==3);
  call("java.render.Camera","activate","(Ljava.render.Viewport;I)V",{Value::make_obj(nullptr),F(5)},&camera);
  SLRR_REQUIRE(gameref_get(&camera)->cam_pri==5);
  call("java.render.Camera","destroy","()V",{},&camera);
  auto time=call("java.lang.String","timeToString","(FI)Ljava.lang.String;",{I(79),F(2)});
  SLRR_REQUIRE(time.tag==ValueTag::Obj && time.v.o->str==format_game_time(79,2));
  time=call("java.lang.String","timeToString","(F)Ljava.lang.String;",{Value::make_obj(&object)});
  SLRR_REQUIRE(time.v.o->str==format_game_time(0,0));
  call("java.lang.System","exit","(Ljava.lang.String;)V",{I(1)});
  SLRR_REQUIRE(f.vm.get_static("java.lang.System","_hostQuit").v.i==1);
  fs::create_directories(f.root/"probe/src");
  std::ofstream(f.root/"probe/src/Safety.java") << R"JAVA(package probe;
public class Safety {
 public static void store(int[] a,int i,int v){a[i]=v;}
 public static void compound(int[] a,int i,int v){a[i]+=v;}
}
)JAVA";
  auto report=script_compile_all(f.root.string(),"probe");
  SLRR_REQUIRE(report.failed==0 && report.succeeded>0);
  fs::path compiled;
  for(auto& entry:fs::recursive_directory_iterator(f.root))if(entry.path().extension()==".class")compiled=entry.path();
  SLRR_REQUIRE(!compiled.empty() && f.vm.load_class_file(compiled.string()));
  Object array;array.is_array=true;array.slots={I(1)};array.fields["length"]=I(1);
  auto store=[&](const char* method,int index,int value){f.vm.invoke("probe.Safety",method,"([III)V",{Value::make_obj(&array),I(index),I(value)},true);};
  store("store",3,9);SLRR_REQUIRE(array.slots.size()==4 && array.slots[3].v.i==9 && array.fields["length"].v.i==4);
  store("compound",3,2);SLRR_REQUIRE(array.slots[3].v.i==11);
  for(int index:{-1,1<<20,INT32_MAX}) {
    store("store",index,99);store("compound",index,99);
    SLRR_REQUIRE(array.slots.size()==4 && array.fields["length"].v.i==4 && array.slots[3].v.i==11);
  }
  store("store",(1<<20)-1,7);
  SLRR_REQUIRE(array.slots.size()==(1<<20) && array.slots.back().v.i==7);
  // The start-up pass: engine script trees only, and only what is older than its
  // container, so an edited source is rebuilt once and an untouched tree is not.
  {
    fs::create_directories(f.root/"sl/Scripts/probe/src");
    const auto source=f.root/"sl/Scripts/probe/src/Stale.java";
    std::ofstream(source)<<"package java.probe;\npublic class Stale { public static int value(){return 7;} }\n";
    const auto container=f.root/"sl/Scripts/probe/Stale.class";
    auto first=script_compile_stale(f.root.string());
    SLRR_REQUIRE(first.attempted==1 && first.succeeded==1 && fs::exists(container));
    auto unchanged=script_compile_stale(f.root.string());
    SLRR_REQUIRE(unchanged.attempted==0 && unchanged.failed==0);
    fs::last_write_time(container,fs::last_write_time(source)-std::chrono::seconds(5));
    auto edited=script_compile_stale(f.root.string());
    SLRR_REQUIRE(edited.attempted==1 && edited.succeeded==1);
    fs::create_directories(f.root/"parts/Mod/scripts/src");
    const auto mod_source=f.root/"parts/Mod/scripts/src/Private.java";
    std::ofstream(mod_source)<<"public class Private { public static int value(){return 3;} }\n";
    auto mods=script_compile_stale(f.root.string());
    SLRR_REQUIRE(mods.attempted==0);
    // Loading a container whose source is newer rebuilds it first, wherever it
    // lives: a mod's private tree is compiled when the game actually asks for it.
    const auto mod_class=f.root/"parts/Mod/scripts/Private.class";
    SLRR_REQUIRE(!fs::exists(mod_class));
    SLRR_REQUIRE(f.vm.load_class_file(mod_class.string()) && fs::exists(mod_class));
    const auto produced=fs::last_write_time(mod_class);
    fs::last_write_time(mod_source,produced+std::chrono::seconds(5));
    SLRR_REQUIRE(f.vm.load_class_file(mod_class.string()));
    SLRR_REQUIRE(fs::last_write_time(mod_class)>produced);
  }
  std::puts("PASS native short/mistagged/boxed arguments; bounded direct and compound array stores");
}
