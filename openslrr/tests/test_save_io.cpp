#include "standalone_test.hpp"
#include "natives_internal.hpp"
#include "slrr/natives_boot.hpp"

#include <filesystem>
#include <fstream>
#include <string>
#include <vector>
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>

namespace {
namespace fs = std::filesystem;
struct Fixture {
  fs::path base, root;
  slrr::NativeRegistry registry;
  slrr::Jvm vm{&registry};
  slrr::RpkIndex packs;
  slrr::SectionMachine sections{&vm};
  slrr::Object file, name;
  Fixture() {
    static unsigned serial = 0;
    base = fs::absolute("build/save-io-fixtures");
    root = base / ("test-" + std::to_string(GetCurrentProcessId()) + "-" + std::to_string(++serial));
    SLRR_REQUIRE(!fs::exists(root));
    fs::create_directories(root);
    vm.set_game_root(root.string());
    packs.set_root(root.string());
    slrr::register_io_natives(vm, packs, sections);
    file.fields["name"] = slrr::Value::make_obj(&name);
  }
  ~Fixture() {
    slrr::g_host_files.erase(&file);
    // Cleanup is restricted to this process's isolated test directory.
    if (root.parent_path() == base && root.filename().string().starts_with("test-")) {
      std::error_code ec;
      fs::remove_all(root, ec);
    }
  }
  slrr::Value invoke(const char* method, const char* signature,
                     const std::vector<slrr::Value>& args = {}, slrr::Object* receiver = nullptr) {
    return registry.invoke("java.io.File", method, signature, receiver, args);
  }
  int erase(const std::string& relative) {
    slrr::Object path; path.str = relative;
    return invoke("delete", "(Ljava.lang.String;)I", {slrr::Value::make_obj(&path)}).v.i;
  }
  int open(const std::string& relative, int mode) {
    name.str = relative;
    return invoke("open", "(I)I", {slrr::Value::make_int(mode)}, &file).v.i;
  }
  int write_int(int value) {return invoke("write", "(I)I", {slrr::Value::make_int(value)}, &file).v.i;}
  void close() {invoke("close", "()V", {}, &file);}
  int read_int() {return invoke("readInt", "()I", {}, &file).v.i;}
  int read_resource() {return invoke("readResID", "()I", {}, &file).v.i;}
  int write_resource(int32_t id, int restype = 0) {
    slrr::Object resource;
    resource.fields["id"] = slrr::Value::make_int(id);
    resource.fields["typeof"] = slrr::Value::make_int(restype);
    return invoke("write", "(Ljava.util.resource.ResourceRef;)I",
                  {slrr::Value::make_obj(&resource)}, &file).v.i;
  }
  void sentinel(const fs::path& path) {
    fs::create_directories(path.parent_path());
    std::ofstream out(path, std::ios::binary); out << "keep";
    SLRR_REQUIRE(bool(out));
  }
};
void invalid_resource_roundtrip(int32_t id, int restype) {
  Fixture f;
  SLRR_REQUIRE(f.open("save/career/main", 1) == 1);
  SLRR_REQUIRE(f.write_int(0x11223344) == 4);
  SLRR_REQUIRE(f.write_resource(id, restype) == 0); // failure remains visible
  SLRR_REQUIRE(f.write_int(0x55667788) == 4);
  slrr::Object text; text.str = "after-resource";
  f.invoke("write", "(Ljava.lang.String;)I", {slrr::Value::make_obj(&text)}, &f.file);
  const auto& payload = slrr::g_host_files.at(&f.file);
  SLRR_REQUIRE(payload.bytes.size() == 12 + 4 + text.str.size() + 1);
  f.close();
  SLRR_REQUIRE(f.open("save/career/main", 0) == 1);
  SLRR_REQUIRE(f.read_int() == 0x11223344);
  SLRR_REQUIRE(f.read_resource() == 0);
  SLRR_REQUIRE(f.read_int() == 0x55667788);
  const auto restored = f.invoke("readString", "()Ljava.lang.String;", {}, &f.file);
  SLRR_REQUIRE(restored.tag == slrr::ValueTag::Obj && restored.v.o);
  SLRR_REQUIRE(restored.v.o->str == text.str);
  f.close();
}
} // namespace

SLRR_TEST(game_root_cannot_be_deleted_or_opened_as_a_file) {
  Fixture f;
  f.sentinel(f.root / "system/keep.txt");
  for (const auto& path : {".", "./", ".\\.", "save/..", "/"}) {
    SLRR_REQUIRE(f.erase(path) == 0);
    SLRR_REQUIRE(fs::exists(f.root / "system/keep.txt"));
    SLRR_REQUIRE(f.open(path, 1) == 0);
  }
}
SLRR_TEST(nonempty_directories_are_not_deleted_recursively) {
  Fixture f;
  f.sentinel(f.root / "save/career/main");
  SLRR_REQUIRE(f.erase("save/career") == 0);
  SLRR_REQUIRE(fs::exists(f.root / "save/career/main"));
}
SLRR_TEST(stock_delete_contents_then_directory_still_works) {
  Fixture f;
  f.sentinel(f.root / "save/career/main");
  SLRR_REQUIRE(f.erase("save/career/main") == 1);
  SLRR_REQUIRE(f.erase("save/career") == 1);
  SLRR_REQUIRE(f.erase("save/missing") == 1);
  SLRR_REQUIRE(fs::is_directory(f.root));
}
SLRR_TEST(double_dots_inside_names_are_legal_but_traversal_is_rejected) {
  Fixture f;
  SLRR_REQUIRE(!slrr::under_game(f.root.string(), "save/J..R-1/main").empty());
  SLRR_REQUIRE(!slrr::under_game(f.root.string(), "save/Mr...-1/main").empty());
  for (const auto& path : {"../outside", "save/../../outside", "save\\..\\outside", "C:\\outside"})
    SLRR_REQUIRE(slrr::under_game(f.root.string(), path).empty());
}
SLRR_TEST(dotted_career_names_roundtrip_through_file_natives) {
  Fixture f;
  for (const auto& name : {"J..R-1", "Mr...-1", "normal-1"}) {
    const std::string path = std::string("save/career/") + name + "/main";
    SLRR_REQUIRE(f.open(path, 1) == 1);
    SLRR_REQUIRE(f.write_int(1234567) == 4);
    f.close();
    SLRR_REQUIRE(f.open(path, 0) == 1);
    SLRR_REQUIRE(f.read_int() == 1234567);
    f.close();
    SLRR_REQUIRE(f.erase(path) == 1);
  }
}
SLRR_TEST(unknown_pack_resource_keeps_following_save_fields_aligned) {invalid_resource_roundtrip(0x12340056, 0);}
SLRR_TEST(packless_resource_keeps_following_save_fields_aligned) {invalid_resource_roundtrip(0x56, 0);}
SLRR_TEST(unsupported_blob_resource_keeps_following_save_fields_aligned) {invalid_resource_roundtrip(int32_t(0xffff0056u), 1);}
SLRR_TEST(valid_and_null_resources_still_roundtrip) {
  Fixture f;
  const auto pack = f.packs.open_lib("fixture.rpk");
  const auto id = int32_t(slrr::RpkIndex::make_full_id(pack, 0x56));
  SLRR_REQUIRE(f.open("save/valid", 1) == 1);
  SLRR_REQUIRE(f.write_resource(id) == 4);
  SLRR_REQUIRE(f.write_resource(0) == 4);
  SLRR_REQUIRE(f.write_int(321) == 4);
  f.close();
  SLRR_REQUIRE(f.open("save/valid", 0) == 1);
  SLRR_REQUIRE(f.read_resource() == id);
  SLRR_REQUIRE(f.read_resource() == 0);
  SLRR_REQUIRE(f.read_int() == 321);
  f.close();
}
SLRR_TEST(raw_file_resource_write_keeps_its_original_id) {
  Fixture f;
  SLRR_REQUIRE(f.open("save/raw", 1) == 1);
  slrr::g_host_files.at(&f.file).sdat = false;
  SLRR_REQUIRE(f.write_resource(0x12340056) == 4);
  f.close();
  SLRR_REQUIRE(f.open("save/raw", 0) == 1);
  SLRR_REQUIRE(f.read_resource() == 0x12340056);
  f.close();
}

SLRR_TEST(findfile_never_enumerates_outside_the_game_root) {
  Fixture f;
  f.sentinel(f.root / "system/keep.txt");
  fs::create_directories(f.base / "outside-game");
  slrr::Object file;
  const auto first = [&](const std::string& pattern) {
    slrr::Object pattern_object;
    pattern_object.str = pattern;
    slrr::Value hit = f.registry.invoke(
        "java.io.FindFile", "first", "(Ljava.lang.String;)Ljava.lang.String;",
        &file, {slrr::Value::make_obj(&pattern_object)});
    return hit.tag == slrr::ValueTag::Obj && hit.v.o ? hit.v.o->str
                                                     : std::string();
  };
  // A pattern may not climb out of the root. The old code concatenated the
  // directory onto the root and let ".." escape.
  for (const auto& pattern :
       {"..\\outside-game\\*", "save/../../outside-game/*",
        "..\\*"})
    SLRR_REQUIRE(first(pattern).empty());
  SLRR_REQUIRE(file.find_entries.empty());
}

int main() {
  const slrr::standalone::Test tests[] = {
    game_root_cannot_be_deleted_or_opened_as_a_file,
    nonempty_directories_are_not_deleted_recursively,
    stock_delete_contents_then_directory_still_works,
    double_dots_inside_names_are_legal_but_traversal_is_rejected,
    dotted_career_names_roundtrip_through_file_natives,
    unknown_pack_resource_keeps_following_save_fields_aligned,
    packless_resource_keeps_following_save_fields_aligned,
    unsupported_blob_resource_keeps_following_save_fields_aligned,
    valid_and_null_resources_still_roundtrip,
    raw_file_resource_write_keeps_its_original_id,
    findfile_never_enumerates_outside_the_game_root,
  };
  return slrr::standalone::run("save_io", tests);
}
