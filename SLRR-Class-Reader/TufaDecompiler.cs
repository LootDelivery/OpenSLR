// C# port of the TUFA/TREE parser and decompiler from the LootDelivery fork of
// SLRR-JVM-decompiler. The original project was created by nedobylskiy.
// See THIRD-PARTY-NOTICES.txt for upstream attribution and MIT license terms.

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace SLRRClassReader
{
    internal sealed class TufaConstant
    {
        public string Kind;
        public string Text;
        public uint A;
        public uint B;
    }

    internal sealed class TufaMember
    {
        public uint Flags;
        public uint Name;
        public uint Descriptor;
        public uint Tree;
        public uint Locals;
    }

    internal sealed class TufaTreeNode
    {
        public int Index;
        public int Offset;
        public int Op;
        public int Line;
        public uint Immediate;
        public bool HasImmediate;
    }

    internal sealed class TufaReference
    {
        public string Owner;
        public string Name;
        public string Descriptor;
    }

    internal sealed class TufaResource
    {
        public string Pack;
        public uint Id;
    }

    internal sealed class TufaClass
    {
        public uint Build;
        public List<TufaConstant> Constants = new List<TufaConstant>();
        public List<TufaMember> StaticFields = new List<TufaMember>();
        public List<TufaMember> InstanceFields = new List<TufaMember>();
        public List<TufaMember> StaticMethods = new List<TufaMember>();
        public List<TufaMember> InstanceMethods = new List<TufaMember>();
        public uint NameIndex;
        public uint SuperIndex;
        public List<uint> InterfaceIndices = new List<uint>();
        public bool IsInterface;
        public List<List<TufaTreeNode>> Trees = new List<List<TufaTreeNode>>();

        public string Utf8(uint index)
        {
            if (index >= Constants.Count || Constants[(int)index].Kind != "utf8")
                throw new InvalidDataException("Expected UTF8 constant " + index.ToString(CultureInfo.InvariantCulture));
            return Constants[(int)index].Text;
        }

        public string ClassName(uint index)
        {
            if (index >= Constants.Count || Constants[(int)index].Kind != "class")
                throw new InvalidDataException("Expected class constant " + index.ToString(CultureInfo.InvariantCulture));
            return Utf8(Constants[(int)index].A);
        }

        public TufaReference Reference(uint index)
        {
            if (index >= Constants.Count || Constants[(int)index].Kind != "reference")
                throw new InvalidDataException("Expected reference constant " + index.ToString(CultureInfo.InvariantCulture));
            TufaConstant item = Constants[(int)index];
            if (item.B >= Constants.Count || Constants[(int)item.B].Kind != "nameAndType")
                throw new InvalidDataException("Expected name/type constant " + item.B.ToString(CultureInfo.InvariantCulture));
            TufaConstant nat = Constants[(int)item.B];
            return new TufaReference { Owner = ClassName(item.A), Name = Utf8(nat.A), Descriptor = Utf8(nat.B) };
        }

        public TufaResource Resource(uint index)
        {
            if (index >= Constants.Count || Constants[(int)index].Kind != "resource")
                throw new InvalidDataException("Expected resource constant " + index.ToString(CultureInfo.InvariantCulture));
            TufaConstant item = Constants[(int)index];
            return new TufaResource { Pack = Utf8(item.A), Id = item.B };
        }

        public string Name { get { return ClassName(NameIndex); } }
        public string SuperName { get { return SuperIndex == UInt32.MaxValue ? null : ClassName(SuperIndex); } }
    }

    internal sealed class TufaReader
    {
        private readonly byte[] bytes;
        private readonly string label;
        private int offset;

        public TufaReader(byte[] data, string name)
        {
            bytes = data;
            label = name;
        }

        public int Remaining { get { return bytes.Length - offset; } }
        public int Offset { get { return offset; } }

        private void Need(int count)
        {
            if (count < 0 || count > Remaining)
                throw new InvalidDataException(label + ": truncated at 0x" + offset.ToString("x", CultureInfo.InvariantCulture));
        }

        public byte ReadByte()
        {
            Need(1);
            return bytes[offset++];
        }

        public ushort ReadUInt16()
        {
            Need(2);
            ushort value = (ushort)(bytes[offset] | (bytes[offset + 1] << 8));
            offset += 2;
            return value;
        }

        public uint ReadUInt32()
        {
            Need(4);
            uint value = (uint)(bytes[offset] | (bytes[offset + 1] << 8) | (bytes[offset + 2] << 16) | (bytes[offset + 3] << 24));
            offset += 4;
            return value;
        }

        public byte[] ReadBytes(int count)
        {
            Need(count);
            byte[] value = new byte[count];
            Buffer.BlockCopy(bytes, offset, value, 0, count);
            offset += count;
            return value;
        }

        public string ReadTag()
        {
            return Encoding.ASCII.GetString(ReadBytes(4));
        }

        public void EnsureDone()
        {
            if (Remaining != 0)
                throw new InvalidDataException(label + ": " + Remaining.ToString(CultureInfo.InvariantCulture) + " trailing bytes");
        }

        public int ReadCount(int minimumItemSize)
        {
            uint value = ReadUInt32();
            if (minimumItemSize <= 0 || value > (uint)(Remaining / minimumItemSize))
                throw new InvalidDataException(label + ": invalid count " + value.ToString(CultureInfo.InvariantCulture));
            return checked((int)value);
        }
    }

    internal static class TufaParser
    {
        private static readonly uint[] OpcodeFlags = new uint[] {
            0x0000,0x1001,0x1002,0x1003,0x0004,0x0005,0x1006,0x1007,
            0x1008,0x4009,0x200a,0x100b,0x000c,0x100d,0x100e,0x100f,
            0x0010,0x1011,0x4012,0x0013,0x1014,0x1015,0x1016,0x1017,
            0x0018,0x1019,0x101a,0x101b,0x401c,0x001d,0x101e,0x101f,
            0x0020,0x0021,0x0022,0x0023,0x0024,0x4025,0x4026,0x1027,
            0x0028,0x0029,0x002a,0x002b,0x002c,0x002d,0x402e,0x0000
        };

        public static List<TufaClass> Parse(byte[] input)
        {
            TufaReader reader = new TufaReader(input, "TUFA");
            List<TufaClass> result = new List<TufaClass>();
            while (reader.Remaining > 0)
            {
                if (reader.ReadTag() != "TUFA" || reader.ReadUInt32() != 4)
                    throw new InvalidDataException("Expected TUFA header");
                uint build = reader.ReadUInt32();
                Dictionary<string, byte[]> chunks = new Dictionary<string, byte[]>();
                while (!chunks.ContainsKey("TREE"))
                {
                    string tag = reader.ReadTag();
                    uint rawLength = reader.ReadUInt32();
                    if (rawLength > Int32.MaxValue)
                        throw new InvalidDataException("Chunk " + tag + " is too large");
                    if (chunks.ContainsKey(tag) || (tag != "CONS" && tag != "FILD" && tag != "MTHD" && tag != "CLSS" && tag != "TREE"))
                        throw new InvalidDataException("Unexpected chunk " + tag);
                    chunks.Add(tag, reader.ReadBytes((int)rawLength));
                }

                if (!chunks.ContainsKey("CONS") || !chunks.ContainsKey("CLSS") || !chunks.ContainsKey("TREE"))
                    throw new InvalidDataException("TUFA blob missing a required chunk");

                TufaClass cls = new TufaClass();
                cls.Build = build;
                cls.Constants = ParseConstants(chunks["CONS"]);
                if (chunks.ContainsKey("FILD"))
                {
                    List<TufaMember> statics, instances;
                    ParseMembers(chunks["FILD"], false, out statics, out instances);
                    cls.StaticFields = statics;
                    cls.InstanceFields = instances;
                }
                if (chunks.ContainsKey("MTHD"))
                {
                    List<TufaMember> statics, instances;
                    ParseMembers(chunks["MTHD"], true, out statics, out instances);
                    cls.StaticMethods = statics;
                    cls.InstanceMethods = instances;
                }
                ParseClassInfo(chunks["CLSS"], cls);
                cls.Trees = ParseTrees(chunks["TREE"]);

                // Resolve references and tree indices while parser context is still available.
                string ignoredName = cls.Name;
                foreach (uint index in cls.InterfaceIndices) cls.ClassName(index);
                foreach (TufaMember member in cls.StaticFields.Concat(cls.InstanceFields).Concat(cls.StaticMethods).Concat(cls.InstanceMethods))
                {
                    cls.Utf8(member.Name);
                    cls.Utf8(member.Descriptor);
                    if (member.Tree != UInt32.MaxValue && member.Tree >= cls.Trees.Count)
                        throw new InvalidDataException(cls.Name + ": tree " + member.Tree.ToString(CultureInfo.InvariantCulture) + " is missing");
                }
                result.Add(cls);
            }
            return result;
        }

        private static List<TufaConstant> ParseConstants(byte[] bytes)
        {
            TufaReader r = new TufaReader(bytes, "CONS");
            int count = r.ReadCount(4);
            List<TufaConstant> values = new List<TufaConstant>(count);
            // Kind-0 CONS entries contain UTF-8 text, not Latin-1 bytes.
            Encoding utf8 = new UTF8Encoding(false, true);
            for (int i = 0; i < count; i++)
            {
                uint kind = r.ReadUInt32();
                TufaConstant item = new TufaConstant();
                if (kind == 0)
                {
                    uint length = r.ReadUInt32();
                    if (length > Int32.MaxValue) throw new InvalidDataException("CONS[" + i + "]: string is too large");
                    item.Kind = "utf8";
                    item.Text = utf8.GetString(r.ReadBytes((int)length));
                    if (r.ReadByte() != 0) throw new InvalidDataException("CONS[" + i + "]: missing NUL");
                }
                else if (kind == 3)
                {
                    item.Kind = "resource"; item.A = r.ReadUInt32(); item.B = r.ReadUInt32();
                }
                else if (kind == 4)
                {
                    item.Kind = "class"; item.A = r.ReadUInt32();
                }
                else if (kind == 5)
                {
                    item.Kind = "reference"; item.A = r.ReadUInt32(); item.B = r.ReadUInt32();
                }
                else if (kind == 7)
                {
                    item.Kind = "nameAndType"; item.A = r.ReadUInt32(); item.B = r.ReadUInt32();
                }
                else throw new InvalidDataException("CONS[" + i + "]: unknown constant kind " + kind.ToString(CultureInfo.InvariantCulture));
                values.Add(item);
            }
            r.EnsureDone();
            return values;
        }

        private static void ParseMembers(byte[] bytes, bool methods, out List<TufaMember> statics, out List<TufaMember> instances)
        {
            TufaReader r = new TufaReader(bytes, methods ? "MTHD" : "FILD");
            int itemSize = methods ? 20 : 16;
            Func<List<TufaMember>> readPartition = delegate()
            {
                int count = r.ReadCount(itemSize);
                List<TufaMember> list = new List<TufaMember>(count);
                for (int i = 0; i < count; i++)
                {
                    TufaMember member = new TufaMember();
                    member.Flags = r.ReadUInt32();
                    member.Name = r.ReadUInt32();
                    member.Descriptor = r.ReadUInt32();
                    member.Tree = r.ReadUInt32();
                    if (methods) member.Locals = r.ReadUInt32();
                    list.Add(member);
                }
                return list;
            };
            statics = readPartition();
            instances = readPartition();
            r.EnsureDone();
        }

        private static void ParseClassInfo(byte[] bytes, TufaClass cls)
        {
            TufaReader r = new TufaReader(bytes, "CLSS");
            if (r.ReadUInt32() != UInt32.MaxValue) throw new InvalidDataException("Invalid CLSS marker");
            cls.IsInterface = r.ReadUInt32() != 0;
            cls.NameIndex = r.ReadUInt32();
            cls.SuperIndex = r.ReadUInt32();
            int count = r.ReadCount(4);
            for (int i = 0; i < count; i++) cls.InterfaceIndices.Add(r.ReadUInt32());
            r.EnsureDone();
        }

        private static List<List<TufaTreeNode>> ParseTrees(byte[] bytes)
        {
            TufaReader r = new TufaReader(bytes, "TREE");
            int count = r.ReadCount(4);
            List<List<TufaTreeNode>> trees = new List<List<TufaTreeNode>>(count);
            for (int i = 0; i < count; i++)
            {
                int length = r.ReadCount(3);
                List<TufaTreeNode> nodes = new List<TufaTreeNode>(length);
                for (int j = 0; j < length; j++)
                {
                    int offset = r.Offset;
                    int op = r.ReadByte();
                    if (op >= OpcodeFlags.Length || op == 0 || op == 47)
                        throw new InvalidDataException("TREE " + i + ":" + j + ": invalid opcode " + op);
                    TufaTreeNode node = new TufaTreeNode();
                    node.Index = j; node.Offset = offset; node.Op = op; node.Line = r.ReadUInt16();
                    if ((OpcodeFlags[op] >> 12) != 0) { node.Immediate = r.ReadUInt32(); node.HasImmediate = true; }
                    nodes.Add(node);
                }
                trees.Add(nodes);
            }
            r.EnsureDone();
            return trees;
        }

        public static bool RecoverMissingClassName(TufaClass cls, string filenameStem)
        {
            if (!String.IsNullOrEmpty(cls.Name)) return false;
            if (!Regex.IsMatch(filenameStem, "^[A-Za-z_$][\\w$]*$"))
                throw new InvalidDataException("Cannot infer a class name from " + filenameStem);
            if (cls.NameIndex >= cls.Constants.Count || cls.Constants[(int)cls.NameIndex].Kind != "class")
                throw new InvalidDataException("Invalid own-class constant");
            uint utf8Index = cls.Constants[(int)cls.NameIndex].A;
            if (utf8Index >= cls.Constants.Count || cls.Constants[(int)utf8Index].Kind != "utf8")
                throw new InvalidDataException("Invalid own-class name constant");
            cls.Constants[(int)utf8Index].Text = filenameStem;
            return true;
        }
    }

    internal sealed class IrInstruction
    {
        public int Index;
        public int Line;
        public int Op;
        public string Name;
        public uint Raw;
        public bool HasRaw;
        public object Operand;
        public int? Target;
    }

    internal sealed class IrBlock
    {
        public int Id;
        public int Start;
        public int End;
        public List<IrInstruction> Instructions = new List<IrInstruction>();
        public List<int> Successors = new List<int>();
        public List<int> Predecessors = new List<int>();
    }

    internal sealed class IrMethod
    {
        public string ClassName;
        public string MethodName;
        public string Descriptor;
        public int TreeIndex;
        public List<IrInstruction> Instructions = new List<IrInstruction>();
        public List<IrBlock> Blocks = new List<IrBlock>();
    }

    internal static class TufaIr
    {
        public static readonly string[] Opcodes = new string[] {
            "invalid","local","declare","release","and_test","or_test","type","expr",
            "stmt","str","float","int","null","bool","rid","unused","end","path",
            "name","invalid","jump","jump_false","jump_true","case","pop","class",
            "method","field","getfield","unused","array_init","push_zeros","index",
            "new_init","this_init","super_init","call","call_on","call_super","push_int",
            "value","push_void","ret","ret_value","init","store","unused","invalid"
        };

        public static readonly string[] Tags = new string[] {
            "cond","lor","land","bor","bxor","band","ne","eq","ge","le","gt","lt",
            "unused","shr","shl","sub","add","mod","div","mul","bnot","lnot","neg",
            "postinc","postdec","preinc","predec","call","literal","unused","new",
            "newarray","instanceof","cast","ctor","assign","mul_assign","div_assign",
            "mod_assign","add_assign","sub_assign","unused","unused","unused",
            "and_assign","xor_assign","or_assign"
        };

        private static readonly uint[] OpcodeFlags = new uint[] {
            0x0000,0x1001,0x1002,0x1003,0x0004,0x0005,0x1006,0x1007,
            0x1008,0x4009,0x200a,0x100b,0x000c,0x100d,0x100e,0x100f,
            0x0010,0x1011,0x4012,0x0013,0x1014,0x1015,0x1016,0x1017,
            0x0018,0x1019,0x101a,0x101b,0x401c,0x001d,0x101e,0x101f,
            0x0020,0x0021,0x0022,0x0023,0x0024,0x4025,0x4026,0x1027,
            0x0028,0x0029,0x002a,0x002b,0x002c,0x002d,0x402e,0x0000
        };

        public static IrMethod LowerTree(TufaClass cls, int treeIndex, string methodName, string descriptor)
        {
            if (treeIndex < 0 || treeIndex >= cls.Trees.Count) throw new InvalidDataException(cls.Name + ": missing tree " + treeIndex);
            List<TufaTreeNode> tree = cls.Trees[treeIndex];
            IrMethod result = new IrMethod { ClassName = cls.Name, MethodName = methodName, Descriptor = descriptor, TreeIndex = treeIndex };
            foreach (TufaTreeNode node in tree)
            {
                IrInstruction ins = new IrInstruction { Index = node.Index, Line = node.Line, Op = node.Op, Name = node.Op >= 0 && node.Op < Opcodes.Length ? Opcodes[node.Op] : "op" + node.Op };
                if (node.HasImmediate)
                {
                    ins.Raw = node.Immediate; ins.HasRaw = true;
                    if (node.Op == 7 || node.Op == 8)
                        ins.Name += " " + (node.Immediate < Tags.Length ? Tags[(int)node.Immediate] : "tag" + node.Immediate.ToString(CultureInfo.InvariantCulture));
                    if (node.Op >= 20 && node.Op <= 23)
                    {
                        int target = node.Index + unchecked((int)node.Immediate);
                        if (target < 0 || target > tree.Count)
                            throw new InvalidDataException(cls.Name + "." + methodName + ": jump " + node.Index + " leaves tree");
                        ins.Target = target; ins.Operand = unchecked((int)node.Immediate);
                    }
                    else if (node.Op == 6 || node.Op == 9 || node.Op == 18 || node.Op == 28 || node.Op == 37 || node.Op == 38 || node.Op == 46)
                        ins.Operand = cls.Utf8(node.Immediate);
                    else if (node.Op == 14) ins.Operand = cls.Resource(node.Immediate);
                    else if (node.Op == 25) ins.Operand = cls.ClassName(node.Immediate);
                    else if (node.Op == 26 || node.Op == 27) ins.Operand = cls.Reference(node.Immediate);
                    else if (node.Op == 10) ins.Operand = BitConverter.ToSingle(BitConverter.GetBytes(node.Immediate), 0);
                    else if (node.Op == 11 || node.Op == 39) ins.Operand = unchecked((int)node.Immediate);
                    else ins.Operand = node.Immediate;
                }
                result.Instructions.Add(ins);
            }
            result.Blocks = BuildBlocks(result.Instructions);
            return result;
        }

        private static List<IrBlock> BuildBlocks(List<IrInstruction> instructions)
        {
            if (instructions.Count == 0) return new List<IrBlock>();
            SortedSet<int> ends = new SortedSet<int>();
            ends.Add(0); ends.Add(instructions.Count);
            foreach (IrInstruction ins in instructions)
            {
                if (ins.Target.HasValue) { ends.Add(ins.Target.Value); ends.Add(ins.Index + 1); }
                if (ins.Op == 16 || ins.Op == 42 || ins.Op == 43) ends.Add(ins.Index + 1);
            }
            List<int> sorted = ends.ToList();
            List<IrBlock> blocks = new List<IrBlock>();
            for (int i = 0; i < sorted.Count - 1; i++)
            {
                int start = sorted[i], end = sorted[i + 1];
                if (start >= end) continue;
                IrBlock block = new IrBlock { Id = blocks.Count, Start = start, End = end };
                block.Instructions = instructions.Skip(start).Take(end - start).ToList();
                blocks.Add(block);
            }
            Dictionary<int, IrBlock> byStart = blocks.ToDictionary(b => b.Start, b => b);
            foreach (IrBlock block in blocks)
            {
                IrInstruction last = block.Instructions[block.Instructions.Count - 1];
                IrBlock target;
                if (last.Target.HasValue && byStart.TryGetValue(last.Target.Value, out target)) block.Successors.Add(target.Id);
                IrBlock next;
                if (last.Op != 20 && last.Op != 16 && last.Op != 42 && last.Op != 43 && byStart.TryGetValue(block.End, out next) && !block.Successors.Contains(next.Id))
                    block.Successors.Add(next.Id);
            }
            foreach (IrBlock block in blocks)
                foreach (int id in block.Successors) blocks[id].Predecessors.Add(block.Id);
            return blocks;
        }

        public static string Format(IrMethod ir)
        {
            HashSet<int> labels = new HashSet<int>(ir.Blocks.Select(b => b.Start));
            List<string> output = new List<string>();
            output.Add(ir.ClassName + "." + ir.MethodName + ir.Descriptor + " / TREE " + ir.TreeIndex);
            foreach (IrBlock block in ir.Blocks)
                output.Add("  B" + block.Id + ": L" + block.Start + "..L" + block.End + " -> " + (block.Successors.Count == 0 ? "exit" : String.Join(", ", block.Successors.Select(x => "B" + x).ToArray())));
            foreach (IrInstruction ins in ir.Instructions)
            {
                if (labels.Contains(ins.Index)) output.Add("L" + ins.Index + ":");
                string operand = ins.Operand == null ? "" : " " + (ins.Operand is TufaReference ? "{" + ((TufaReference)ins.Operand).Owner + "," + ((TufaReference)ins.Operand).Name + "}" : ins.Operand is TufaResource ? "{" + ((TufaResource)ins.Operand).Pack + "," + ((TufaResource)ins.Operand).Id + "}" : Convert.ToString(ins.Operand, CultureInfo.InvariantCulture));
                string target = ins.Target.HasValue ? " -> L" + ins.Target.Value : "";
                output.Add("  " + ins.Index.ToString().PadLeft(4) + "  src:" + ins.Line.ToString().PadLeft(4) + "  " + ins.Name + operand + target);
            }
            if (labels.Contains(ir.Instructions.Count)) output.Add("L" + ir.Instructions.Count + ":");
            return String.Join("\n", output.ToArray());
        }
    }

    internal sealed class DecompileResult
    {
        public string Source;
        public List<string> Diagnostics = new List<string>();
        public List<IrMethod> Intermediate = new List<IrMethod>();
    }

    internal sealed class DecodedMethod
    {
        public List<string> Parameters = new List<string>();
        public string Returns;
    }

    internal sealed class PendingLocal
    {
        public uint Slot;
        public string Type;
        public string Name;
    }

    internal sealed class PathResult
    {
        public string Value;
        public int Next;
    }

    internal sealed class TypeNames
    {
        public HashSet<string> Imports = new HashSet<string>();
        private Dictionary<string, HashSet<string>> bySimple = new Dictionary<string, HashSet<string>>();

        public TypeNames(TufaClass cls)
        {
            Add(cls.Name);
            if (cls.SuperName != null) Add(cls.SuperName);
            foreach (uint index in cls.InterfaceIndices) Add(cls.ClassName(index));
            for (int i = 0; i < cls.Constants.Count; i++)
            {
                TufaConstant entry = cls.Constants[i];
                if (entry.Kind == "class") Add(cls.ClassName((uint)i));
                if (entry.Kind == "reference") Add(cls.Reference((uint)i).Owner);
                if (entry.Kind == "utf8")
                    foreach (Match match in Regex.Matches(entry.Text, "L([^;]+);")) Add(match.Groups[1].Value);
            }
            string ownPackage = PackageOf(cls.Name);
            foreach (HashSet<string> set in bySimple.Values)
            {
                if (set.Count != 1) continue;
                string name = set.First();
                string packageName = PackageOf(name);
                if (packageName != ownPackage && packageName != "java.lang") Imports.Add(packageName + ".*");
            }
        }

        private void Add(string name)
        {
            if (name == null || !name.Contains(".") || name.StartsWith("[", StringComparison.Ordinal)) return;
            string shortName = Simple(name);
            HashSet<string> set;
            if (!bySimple.TryGetValue(shortName, out set)) { set = new HashSet<string>(); bySimple.Add(shortName, set); }
            set.Add(name);
        }

        public string Name(string name)
        {
            HashSet<string> set;
            return bySimple.TryGetValue(Simple(name), out set) && set.Count == 1 ? Simple(name) : name;
        }

        public string Type(string type)
        {
            return Regex.Replace(type, "\\b(?:[A-Za-z_]\\w*\\.)+[A-Za-z_]\\w*\\b", delegate(Match m) { return Name(m.Value); });
        }

        public static string Simple(string name)
        {
            int index = name.LastIndexOf('.');
            return index < 0 ? name : name.Substring(index + 1);
        }

        public static string PackageOf(string name)
        {
            int index = name.LastIndexOf('.');
            return index < 0 ? "" : name.Substring(0, index);
        }
    }

    internal sealed class BodyWriter
    {
        private static readonly Dictionary<int, string> Symbols = new Dictionary<int, string> {
            {1,"||"},{2,"&&"},{3,"|"},{4,"^"},{5,"&"},{6,"!="},{7,"=="},{8,">="},{9,"<="},
            {10,">"},{11,"<"},{13,">>"},{14,"<<"},{15,"-"},{16,"+"},{17,"%"},{18,"/"},{19,"*"},
            {35,"="},{36,"*="},{37,"/="},{38,"%="},{39,"+="},{40,"-="},{44,"&="},{45,"^="},{46,"|="}
        };
        private readonly TufaClass cls;
        private readonly TufaMember member;
        private readonly IrMethod ir;
        private readonly TypeNames types;
        private readonly List<IrInstruction> ins;
        private readonly List<string> stack = new List<string>();
        private readonly Dictionary<uint, string> locals = new Dictionary<uint, string>();
        private readonly List<string> lines = new List<string>();
        private PendingLocal pending;
        private readonly Dictionary<uint, int> declarations = new Dictionary<uint, int>();

        public BodyWriter(TufaClass c, TufaMember m, IrMethod method, TypeNames typeNames, bool fieldMode)
        {
            cls = c; member = m; ir = method; types = typeNames; ins = method.Instructions;
            if (fieldMode) return;
            DecodedMethod signature = DecompileEngine.DecodeMethod(cls.Utf8(member.Descriptor));
            bool isStatic = (member.Flags & 8) != 0;
            if (!isStatic) locals[0] = "this";
            for (int i = 0; i < signature.Parameters.Count; i++) locals[(uint)((isStatic ? 0 : 1) + signature.Parameters.Count - 1 - i)] = "arg" + i;
        }

        private InvalidDataException Fail(int pc, string detail)
        {
            return new InvalidDataException(cls.Name + "." + ir.MethodName + " TREE " + ir.TreeIndex + ":" + pc + ": " + detail);
        }
        private string Pop(int pc)
        {
            if (stack.Count == 0) throw Fail(pc, "operand stack underflow");
            string value = stack[stack.Count - 1]; stack.RemoveAt(stack.Count - 1); return value;
        }
        private void Push(string value) { stack.Add(value); }
        private void Emit(int depth, string value) { lines.Add(new String('\t', 0) + new String(' ', depth * 4) + value); }
        private string Local(uint slot) { string name; return locals.TryGetValue(slot, out name) ? name : "local" + slot.ToString(CultureInfo.InvariantCulture); }
        private uint Raw(int pc)
        {
            if (pc < 0 || pc >= ins.Count || !ins[pc].HasRaw) throw Fail(pc, "missing immediate");
            return ins[pc].Raw;
        }
        private string Text(int pc)
        {
            if (pc < 0 || pc >= ins.Count || ins[pc].Operand == null) throw Fail(pc, "missing operand");
            return Convert.ToString(ins[pc].Operand, CultureInfo.InvariantCulture);
        }

        private static string Quote(string value)
        {
            StringBuilder b = new StringBuilder(); b.Append('"');
            foreach (char c in value)
            {
                if (c == '"') b.Append("\\\"");
                else if (c == '\\') b.Append("\\\\");
                else if (c == '\b') b.Append("\\b");
                else if (c == '\f') b.Append("\\f");
                else if (c == '\n') b.Append("\\n");
                else if (c == '\r') b.Append("\\r");
                else if (c == '\t') b.Append("\\t");
                else if (c < 32) b.Append("\\u" + ((int)c).ToString("x4", CultureInfo.InvariantCulture));
                else b.Append(c);
            }
            b.Append('"'); return b.ToString();
        }

        private string Literal(int pc)
        {
            IrInstruction node = ins[pc];
            switch (node.Op)
            {
                case 9: return Quote(Convert.ToString(node.Operand, CultureInfo.InvariantCulture));
                case 10:
                    float f = Convert.ToSingle(node.Operand, CultureInfo.InvariantCulture);
                    string ft = f.ToString("R", CultureInfo.InvariantCulture);
                    if (ft.IndexOf('.') < 0 && ft.IndexOf('E') < 0 && ft.IndexOf('e') < 0) ft += ".0";
                    return ft;
                case 11: return Convert.ToString(node.Operand, CultureInfo.InvariantCulture);
                case 12: return "null";
                case 13: return node.Raw != 0 ? "true" : "false";
                case 14:
                    TufaResource resource = (TufaResource)node.Operand;
                    string pack = Regex.Replace(resource.Pack, "\\.rpk$", "", RegexOptions.IgnoreCase).Replace('\\', '.').Replace('/', '.');
                    return pack + ":0x" + resource.Id.ToString("x", CultureInfo.InvariantCulture) + "r";
                default: throw Fail(pc, "expected literal carrier");
            }
        }

        private PathResult ReadPath(int start)
        {
            int p = start, count = 1;
            if (ins[p].Op == 17) { count = unchecked((int)Raw(p)); p++; }
            if (count <= 0 || count > 128) throw Fail(start, "invalid path count " + count);
            List<string> parts = new List<string>();
            for (int i = 0; i < count; i++, p++)
            {
                if (p >= ins.Count) throw Fail(p, "incomplete path");
                IrInstruction node = ins[p];
                if (node.Op == 1) parts.Add(Local(Raw(p)));
                else if (node.Op == 18) parts.Add(Text(p));
                else if (node.Op == 25) parts.Add(types.Name(Text(p)));
                else if (node.Op == 26 || node.Op == 27) parts.Add(((TufaReference)node.Operand).Name);
                else throw Fail(p, "unexpected path node " + node.Name);
            }
            return new PathResult { Value = String.Join(".", parts.ToArray()), Next = p };
        }

        private List<string> ReadArguments(int pc)
        {
            double number;
            if (!Double.TryParse(Pop(pc), NumberStyles.Float, CultureInfo.InvariantCulture, out number) || number < 0 || number > 128 || number != Math.Floor(number))
                throw Fail(pc, "invalid argument count");
            int count = (int)number;
            List<string> args = new List<string>();
            for (int i = 0; i < count; i++) args.Insert(0, Pop(pc));
            return args;
        }

        private int Expression(int pc, int depth)
        {
            if (pc < 0 || pc >= ins.Count) throw Fail(pc, "missing instruction");
            IrInstruction node = ins[pc];
            switch (node.Op)
            {
                case 1: Push(Local(Raw(pc))); return pc + 1;
                case 2:
                    if (pc + 1 >= ins.Count || ins[pc + 1].Op != 6) throw Fail(pc, "declaration missing type carrier");
                    uint slot = Raw(pc);
                    string type = types.Type(DecompileEngine.DecodeType(Convert.ToString(ins[pc + 1].Operand, CultureInfo.InvariantCulture)));
                    int ordinal; declarations.TryGetValue(slot, out ordinal); ordinal++; declarations[slot] = ordinal;
                    string name = "local" + slot.ToString(CultureInfo.InvariantCulture) + (ordinal > 1 ? "_" + ordinal : "");
                    locals[slot] = name;
                    if (pc + 2 < ins.Count && ins[pc + 2].Op == 44)
                    {
                        pending = new PendingLocal { Slot = slot, Type = type, Name = name };
                        Push(name); return pc + 3;
                    }
                    Emit(depth, type + " " + name + ";"); return pc + 2;
                case 3:
                    string oldName;
                    if (locals.TryGetValue(Raw(pc), out oldName) && oldName.StartsWith("local", StringComparison.Ordinal)) locals.Remove(Raw(pc));
                    return pc + 1;
                case 6: case 9: case 10: case 11: case 12: case 13: case 14: case 18: case 25: case 26: case 44:
                    throw Fail(pc, "orphan carrier " + node.Name);
                case 7: case 8:
                    int tag = unchecked((int)Raw(pc));
                    bool statement = node.Op == 8;
                    if (tag == 28)
                    {
                        string value = Literal(pc + 1);
                        if (!statement) Push(value);
                        return pc + 2;
                    }
                    if (tag == 30)
                    {
                        if (ins[pc + 1].Op != 6) throw Fail(pc, "new missing type");
                        Push("new " + types.Type(DecompileEngine.DecodeType(Text(pc + 1)))); return pc + 2;
                    }
                    if (tag == 31)
                    {
                        if (ins[pc + 1].Op != 6) throw Fail(pc, "new array missing type");
                        string size = Pop(pc), arrayType = types.Type(DecompileEngine.DecodeType(Text(pc + 1)));
                        arrayType = Regex.Replace(arrayType, "\\[\\]$", "");
                        Push("new " + arrayType + "[" + size + "]"); return pc + 2;
                    }
                    if (tag == 32 || tag == 33)
                    {
                        IrInstruction typeNode = ins[pc + 1];
                        if (typeNode.Op != 6 && typeNode.Op != 25) throw Fail(pc, "type operation missing carrier");
                        string castType = types.Type(typeNode.Op == 6 ? DecompileEngine.DecodeType(Text(pc + 1)) : Text(pc + 1));
                        string value = Pop(pc);
                        Push(tag == 32 ? "(" + value + " instanceof " + castType + ")" : "((" + castType + ") " + value + ")");
                        return pc + 2;
                    }
                    if (tag == 34) return pc + 1;
                    if (tag == 27)
                    {
                        string value = Pop(pc);
                        if (statement) Emit(depth, value + ";"); else Push(value);
                        return pc + 1;
                    }
                    if (tag == 23 || tag == 24 || tag == 25 || tag == 26)
                    {
                        string value = Pop(pc), op = tag == 23 || tag == 25 ? "++" : "--";
                        string expression = tag >= 25 ? op + value : value + op;
                        if (statement) Emit(depth, expression + ";"); else Push(expression);
                        return pc + 1;
                    }
                    if (tag == 20 || tag == 21 || tag == 22)
                    {
                        string value = Pop(pc), expression = (tag == 20 ? "~" : tag == 21 ? "!" : "-") + "(" + value + ")";
                        if (statement) Emit(depth, expression + ";"); else Push("(" + expression + ")");
                        return pc + 1;
                    }
                    if (tag == 0)
                    {
                        string no = Pop(pc), yes = Pop(pc), condition = Pop(pc);
                        string expression = "(" + condition + " ? " + yes + " : " + no + ")";
                        if (statement) Emit(depth, expression + ";"); else Push(expression);
                        return pc + 1;
                    }
                    string symbol;
                    if (Symbols.TryGetValue(tag, out symbol))
                    {
                        string right = Pop(pc), left = Pop(pc);
                        bool assignment = tag >= 35;
                        string expression = assignment ? right + " " + symbol + " " + left : "(" + left + " " + symbol + " " + right + ")";
                        if (assignment && pending != null && pending.Name == right)
                        {
                            Emit(depth, pending.Type + " " + right + " " + symbol + " " + left + ";"); pending = null;
                        }
                        else if (statement) Emit(depth, expression + ";");
                        else Push(assignment ? "(" + expression + ")" : expression);
                        return pc + 1;
                    }
                    throw Fail(pc, "unsupported expression tag " + tag);
                case 17:
                    PathResult path = ReadPath(pc); Push(path.Value); return path.Next;
                case 27:
                    TufaReference reference = (TufaReference)node.Operand;
                    bool ownStatic = reference.Owner == cls.Name && cls.StaticFields.Any(f => cls.Utf8(f.Name) == reference.Name);
                    Push(reference.Owner == cls.Name ? (ownStatic ? TypeNames.Simple(cls.Name) + "." + reference.Name : "this." + reference.Name) : types.Name(reference.Owner) + "." + reference.Name);
                    return pc + 1;
                case 28: Push(Pop(pc) + "." + Text(pc)); return pc + 1;
                case 32:
                    string array = Pop(pc), index = Pop(pc); Push(array + "[" + index + "]"); return pc + 1;
                case 33:
                    List<string> objectArgs = ReadArguments(pc); string objectName = Pop(pc); Push(objectName + "(" + String.Join(", ", objectArgs.ToArray()) + ")"); return pc + 1;
                case 34: case 35:
                    List<string> ctorArgs = ReadArguments(pc); Emit(depth, (node.Op == 34 ? "this" : "super") + "(" + String.Join(", ", ctorArgs.ToArray()) + ");"); return pc + 1;
                case 36:
                    List<string> callArgs = ReadArguments(pc); PathResult methodPath = ReadPath(pc + 1); Push(methodPath.Value + "(" + String.Join(", ", callArgs.ToArray()) + ")"); return methodPath.Next;
                case 37:
                    string receiver = Pop(pc); List<string> onArgs = ReadArguments(pc); Push(receiver + "." + Text(pc) + "(" + String.Join(", ", onArgs.ToArray()) + ")"); return pc + 1;
                case 38:
                    List<string> superArgs = ReadArguments(pc); Push("super." + Text(pc) + "(" + String.Join(", ", superArgs.ToArray()) + ")"); return pc + 1;
                case 39: Push(Convert.ToString(node.Operand, CultureInfo.InvariantCulture)); return pc + 1;
                case 40: return pc + 1;
                case 41: Push("/*void*/"); return pc + 1;
                case 45:
                    string storedValue = Pop(pc), destination = Pop(pc);
                    if (pending == null || pending.Name != destination) throw Fail(pc, "unexpected initializer store");
                    Emit(depth, pending.Type + " " + destination + " = " + storedValue + ";"); pending = null; return pc + 1;
                default: throw Fail(pc, "unsupported opcode " + node.Name);
            }
        }

        private int ShortCircuit(int pc, int depth)
        {
            IrInstruction test = ins[pc], jump = ins[pc + 1];
            if (jump.Op != 20 || !jump.Target.HasValue || jump.Target.Value <= pc + 2 || jump.Target.Value > ins.Count)
                throw Fail(pc, "invalid short-circuit jump");
            IrInstruction last = ins[jump.Target.Value - 1];
            if (last.Op != 7 || last.Raw != (test.Op == 4 ? 2u : 1u)) throw Fail(pc, "short-circuit expression missing final operator");
            int rhs = pc + 2;
            while (rhs < jump.Target.Value - 1)
            {
                if (ins[rhs].Op == 4 || ins[rhs].Op == 5) rhs = ShortCircuit(rhs, depth);
                else rhs = Expression(rhs, depth);
            }
            if (rhs != jump.Target.Value - 1) throw Fail(pc, "short-circuit RHS crosses final operator");
            Expression(rhs, depth);
            return jump.Target.Value;
        }

        private int SwitchAt(int pc, int depth, int? loopHeader, int? loopExit)
        {
            List<string> labels = new List<string>();
            List<int> targets = new List<int>();
            int p = pc;
            while (p < ins.Count)
            {
                IrInstruction node = ins[p];
                if (node.Op == 23) { labels.Add(Pop(p)); targets.Add(node.Target.Value); p++; continue; }
                if (node.Op == 24) break;
                if (node.Op == 4 || node.Op == 5) p = ShortCircuit(p, depth); else p = Expression(p, depth);
            }
            if (ins[p].Op != 24 || ins[p + 1].Op != 20 || labels.Count == 0) throw Fail(pc, "malformed switch dispatch");
            string subject = Pop(p);
            int defaultTarget = ins[p + 1].Target.Value;
            int bodyStart = p + 2;
            if (targets.Any(t => t < bodyStart)) throw Fail(pc, "switch case points into dispatch");
            List<int> beyondDefault = ins.Skip(bodyStart).Take(defaultTarget - bodyStart)
                .Where(n => n.Op == 20 && n.Target.HasValue && n.Target.Value > defaultTarget)
                .Select(n => n.Target.Value).ToList();
            int exit = beyondDefault.Count > 0 ? beyondDefault.Max() : defaultTarget;
            if (exit > ins.Count || targets.Any(t => t >= exit)) throw Fail(pc, "cannot delimit switch groups");
            SortedSet<int> startSet = new SortedSet<int>(targets);
            if (defaultTarget < exit) startSet.Add(defaultTarget);
            List<int> starts = startSet.ToList();
            if (starts.Count == 0 || starts[0] != bodyStart) throw Fail(pc, "switch first group is not adjacent to dispatch");
            Emit(depth, "switch (" + subject + ") {");
            for (int i = 0; i < starts.Count; i++)
            {
                int first = starts[i], last = i + 1 < starts.Count ? starts[i + 1] : exit;
                for (int j = 0; j < labels.Count; j++) if (targets[j] == first) Emit(depth + 1, "case " + labels[j] + ":");
                if (first == defaultTarget && defaultTarget < exit) Emit(depth + 1, "default:");
                IrInstruction tail = ins[last - 1];
                bool breaks = tail.Op == 20 && tail.Target.HasValue && tail.Target.Value == exit;
                Region(first, breaks ? last - 1 : last, depth + 2, loopHeader, loopExit, exit, false);
                if (breaks) Emit(depth + 2, "break;");
            }
            Emit(depth, "}");
            return exit;
        }

        private void ScopedRegion(int start, int end, int depth, int? loopHeader, int? loopExit, int? switchExit)
        {
            Dictionary<uint, string> saved = new Dictionary<uint, string>(locals);
            try { Region(start, end, depth, loopHeader, loopExit, switchExit, false); }
            finally { locals.Clear(); foreach (KeyValuePair<uint, string> item in saved) locals[item.Key] = item.Value; }
        }

        private void Region(int start, int end, int depth, int? loopHeader, int? loopExit, int? switchExit, bool insideDo)
        {
            int pc = start;
            while (pc < end)
            {
                if (!insideDo)
                {
                    int bottom = -1;
                    for (int j = pc + 1; j < end; j++)
                        if ((ins[j].Op == 21 || ins[j].Op == 22) && ins[j].Target.HasValue && ins[j].Target.Value == pc) { bottom = j; break; }
                    if (bottom >= 0)
                    {
                        Emit(depth, "do {");
                        Region(pc, bottom, depth + 1, pc, bottom + 1, switchExit, true);
                        string condition = Pop(bottom);
                        Emit(depth, "} while (" + (ins[bottom].Op == 21 ? "!(" + condition + ")" : condition) + ");");
                        pc = bottom + 1; continue;
                    }
                }
                IrInstruction node = ins[pc];
                if (node.Op == 21 || node.Op == 22)
                {
                    string condition = Pop(pc);
                    int target = node.Target.Value;
                    if (target > end || target < start) throw Fail(pc, "branch target " + target + " crosses region");
                    if (target > pc)
                    {
                        IrInstruction tail = ins[target - 1];
                        bool positive = node.Op == 22;
                        string test = positive ? "!(" + condition + ")" : condition;
                        if (tail.Op == 20 && tail.Target.HasValue && tail.Target.Value <= pc && tail.Target.Value >= start)
                        {
                            Emit(depth, "while (" + test + ") {");
                            Region(pc + 1, target - 1, depth + 1, tail.Target.Value, target, switchExit, false);
                            Emit(depth, "}"); pc = target; continue;
                        }
                        if (tail.Op == 20 && tail.Target.HasValue && tail.Target.Value > target && tail.Target.Value <= end)
                        {
                            Emit(depth, "if (" + test + ") {");
                            ScopedRegion(pc + 1, target - 1, depth + 1, loopHeader, loopExit, switchExit);
                            Emit(depth, "} else {");
                            ScopedRegion(target, tail.Target.Value, depth + 1, loopHeader, loopExit, switchExit);
                            Emit(depth, "}"); pc = tail.Target.Value; continue;
                        }
                        Emit(depth, "if (" + test + ") {");
                        ScopedRegion(pc + 1, target, depth + 1, loopHeader, loopExit, switchExit);
                        Emit(depth, "}"); pc = target; continue;
                    }
                    throw Fail(pc, "unstructured backward conditional jump to " + target);
                }
                if (node.Op == 20)
                {
                    int target = node.Target.Value;
                    if (target == pc + 1) { pc++; continue; }
                    if (switchExit.HasValue && target == switchExit.Value) { Emit(depth, "break;"); pc++; continue; }
                    if (loopExit.HasValue && target == loopExit.Value) { Emit(depth, "break;"); pc++; continue; }
                    if (loopHeader.HasValue && target == loopHeader.Value) { Emit(depth, "continue;"); pc++; continue; }
                    throw Fail(pc, "unstructured unconditional jump to " + target);
                }
                if (node.Op == 23) { pc = SwitchAt(pc, depth, loopHeader, loopExit); continue; }
                if (node.Op == 4 || node.Op == 5) { pc = ShortCircuit(pc, depth); continue; }
                if (node.Op == 42)
                {
                    if (!ins.Skip(pc + 1).All(tail => tail.Op == 3)) Emit(depth, "return;");
                    pc++; continue;
                }
                if (node.Op == 43)
                {
                    string value = Pop(pc);
                    bool implicitVoidReturn = value == "/*void*/" && ins.Skip(pc + 1).All(tail => tail.Op == 3);
                    if (!implicitVoidReturn) Emit(depth, value == "/*void*/" ? "return;" : "return " + value + ";");
                    pc++; continue;
                }
                if (node.Op == 16) { pc++; continue; }
                if (node.Op == 24) { Pop(pc); pc++; continue; }
                pc = Expression(pc, depth);
            }
        }

        public List<string> Run()
        {
            int back = -1;
            for (int i = 1; i < ins.Count; i++) if (ins[i].Op == 20 && ins[i].Target.HasValue && ins[i].Target.Value == 0) { back = i; break; }
            bool escape = back >= 0 && ins.Take(back).Any(n => (n.Op == 21 || n.Op == 22) && n.Target.HasValue && n.Target.Value == back + 1);
            if (back >= 0 && !escape)
            {
                Emit(2, "while (true) {");
                Region(0, back, 3, 0, back + 1, null, false);
                Emit(2, "}");
                Region(back + 1, ins.Count, 2, null, null, null, false);
            }
            else Region(0, ins.Count, 2, null, null, null, false);
            return lines;
        }

        public string FieldValue()
        {
            if (ins.Count == 0 || ins[ins.Count - 1].Op != 16) throw Fail(ins.Count - 1, "field initializer missing end marker");
            int pc = 0;
            while (pc < ins.Count - 1)
            {
                if (ins[pc].Op == 4 || ins[pc].Op == 5) pc = ShortCircuit(pc, 0);
                else pc = Expression(pc, 0);
            }
            if (stack.Count != 1 || lines.Count != 0) throw Fail(pc, "field initializer is not a single expression");
            return Pop(pc);
        }
    }

    internal static class DecompileEngine
    {
        private static readonly Tuple<uint, string>[] AccessFlags = new Tuple<uint, string>[] {
            Tuple.Create(1u,"public"),Tuple.Create(2u,"protected"),Tuple.Create(4u,"private"),Tuple.Create(8u,"static"),
            Tuple.Create(16u,"abstract"),Tuple.Create(32u,"final"),Tuple.Create(64u,"native"),Tuple.Create(128u,"synchronized")
        };

        public static string DecodeType(string descriptor)
        {
            int offset = 0;
            string type = TypeAt(descriptor, ref offset);
            if (offset != descriptor.Length) throw new InvalidDataException("Invalid type descriptor " + descriptor);
            return type;
        }

        private static string TypeAt(string descriptor, ref int offset)
        {
            if (offset >= descriptor.Length) throw new InvalidDataException("Invalid type descriptor " + descriptor);
            char c = descriptor[offset];
            if (c == '[') { offset++; return TypeAt(descriptor, ref offset) + "[]"; }
            if (c == 'L')
            {
                int end = descriptor.IndexOf(';', offset);
                if (end < 0) throw new InvalidDataException("Invalid type descriptor " + descriptor);
                string type = end == offset + 1 ? "java.lang.Object" : descriptor.Substring(offset + 1, end - offset - 1).Replace('/', '.');
                offset = end + 1; return type;
            }
            string primitive;
            switch (c)
            {
                case 'V': primitive = "void"; break; case 'I': primitive = "int"; break; case 'F': primitive = "float"; break;
                case 'D': primitive = "double"; break; case 'Z': primitive = "boolean"; break; case 'B': primitive = "byte"; break;
                case 'C': primitive = "char"; break; case 'S': primitive = "short"; break; case 'J': primitive = "long"; break;
                default: throw new InvalidDataException("Invalid type descriptor " + descriptor);
            }
            offset++; return primitive;
        }

        public static DecodedMethod DecodeMethod(string descriptor)
        {
            if (!descriptor.StartsWith("(", StringComparison.Ordinal)) throw new InvalidDataException("Invalid method descriptor " + descriptor);
            int offset = 1;
            DecodedMethod result = new DecodedMethod();
            while (offset < descriptor.Length && descriptor[offset] != ')') result.Parameters.Add(TypeAt(descriptor, ref offset));
            if (offset >= descriptor.Length) throw new InvalidDataException("Invalid method descriptor " + descriptor);
            if (offset + 1 == descriptor.Length) { result.Returns = "void"; return result; }
            offset++;
            result.Returns = TypeAt(descriptor, ref offset);
            if (offset != descriptor.Length) throw new InvalidDataException("Invalid method descriptor " + descriptor);
            return result;
        }

        private static string Flags(uint bits)
        {
            return String.Join(" ", AccessFlags.Where(item => (bits & item.Item1) != 0).Select(item => item.Item2).ToArray());
        }

        private static string DefaultValue(string type)
        {
            if (type == "boolean") return "false";
            if (type == "float") return "0.0";
            if (type == "void") return "";
            if (type == "int" || type == "byte" || type == "short" || type == "long" || type == "double" || type == "char") return "0";
            return "null";
        }

        private static string FieldSource(TufaClass cls, TufaMember member, TypeNames types)
        {
            string type = types.Type(DecodeType(cls.Utf8(member.Descriptor)));
            string name = cls.Utf8(member.Name);
            string init = "";
            if (member.Tree != UInt32.MaxValue)
            {
                IrMethod ir = TufaIr.LowerTree(cls, (int)member.Tree, name, cls.Utf8(member.Descriptor));
                init = " = " + new BodyWriter(cls, member, ir, types, true).FieldValue();
            }
            string flags = Flags(member.Flags);
            return "    " + flags + (flags.Length > 0 ? " " : "") + type + " " + name + init + ";";
        }

        public static DecompileResult DecompileClass(TufaClass cls)
        {
            DecompileResult result = new DecompileResult();
            TypeNames types = new TypeNames(cls);
            string packageName = TypeNames.PackageOf(cls.Name);
            List<string> output = new List<string>();
            if (packageName.Length > 0) { output.Add("package " + packageName + ";"); output.Add(""); }
            foreach (string importName in types.Imports.OrderBy(x => x, StringComparer.Ordinal)) output.Add("import " + importName + ";");
            if (types.Imports.Count > 0) output.Add("");
            string own = TypeNames.Simple(cls.Name);
            string inherited = cls.SuperName != null && cls.SuperName != "java.lang.Object" ? " extends " + types.Name(cls.SuperName) : "";
            List<string> interfaces = cls.InterfaceIndices.Select(index => types.Name(cls.ClassName(index))).ToList();
            string implementsClause = interfaces.Count > 0 ? " " + (cls.IsInterface ? "extends " : "implements ") + String.Join(", ", interfaces.ToArray()) : "";
            output.Add("public " + (cls.IsInterface ? "interface " : "class ") + own + inherited + implementsClause + " {");
            foreach (TufaMember member in cls.StaticFields.Concat(cls.InstanceFields)) output.Add(FieldSource(cls, member, types));
            if (cls.StaticFields.Count + cls.InstanceFields.Count > 0) output.Add("");

            foreach (TufaMember member in cls.StaticMethods.Concat(cls.InstanceMethods))
            {
                string name = cls.Utf8(member.Name), descriptor = cls.Utf8(member.Descriptor);
                DecodedMethod signature = DecodeMethod(descriptor);
                string parameters = String.Join(", ", signature.Parameters.Select((type, index) => types.Type(type) + " arg" + index).ToArray());
                string prefix = Flags(member.Flags);
                string header = "    " + prefix + (prefix.Length > 0 ? " " : "") + (name == "<init>" ? own : types.Type(signature.Returns) + " " + name) + "(" + parameters + ")";
                if (member.Tree == UInt32.MaxValue || (member.Flags & (16 | 64)) != 0)
                {
                    output.Add(header + ";"); output.Add(""); continue;
                }
                IrMethod ir = TufaIr.LowerTree(cls, (int)member.Tree, name, descriptor);
                result.Intermediate.Add(ir);
                output.Add(header + " {");
                try
                {
                    output.AddRange(new BodyWriter(cls, member, ir, types, false).Run());
                }
                catch (Exception error)
                {
                    string message = error.Message.Replace("*/", "* /");
                    result.Diagnostics.Add(error.Message);
                    output.Add("        /* Decompilation incomplete: " + message + " */");
                    foreach (string line in TufaIr.Format(ir).Split('\n')) output.Add("        // " + line);
                    if (signature.Returns != "void" && name != "<init>") output.Add("        return " + DefaultValue(signature.Returns) + ";");
                }
                output.Add("    }"); output.Add("");
            }
            output.Add("}"); output.Add("");
            result.Source = String.Join("\n", output.ToArray());
            return result;
        }
    }

    internal sealed class DecompileTimings
    {
        public double ReadMs;
        public double ParseMs;
        public double DecompileMs;
        public double FormatMs;
        public double CombineMs;
        public double WriteMs;
        public double TotalMs;
    }

    internal sealed class FileDecompileResult
    {
        public string InputPath;
        public string OutputPath;
        public string ClassName;
        public List<string> ClassNames = new List<string>();
        public string Source;
        public List<string> Diagnostics = new List<string>();
        public int ClassCount;
        public bool UsedFilenameFallback;
        public DecompileTimings Timings = new DecompileTimings();
        public string Error;
        public bool Success { get { return Error == null; } }
    }

    internal static class SourceBundle
    {
        private sealed class Unit
        {
            public string PackageLine;
            public List<string> Imports = new List<string>();
            public string Body;
        }

        private static Unit Split(string source)
        {
            Unit unit = new Unit();
            List<string> body = new List<string>();
            foreach (string line in source.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n'))
            {
                string trimmed = line.Trim();
                if (Regex.IsMatch(trimmed, "^package\\s+[^;]+;$"))
                {
                    if (!String.IsNullOrEmpty(unit.PackageLine) && unit.PackageLine != trimmed)
                        throw new InvalidDataException("TUFA source contains conflicting package declarations");
                    unit.PackageLine = trimmed;
                }
                else if (Regex.IsMatch(trimmed, "^import\\s+(?:static\\s+)?[^;]+;$")) unit.Imports.Add(trimmed);
                else body.Add(line);
            }
            unit.Body = String.Join("\n", body.ToArray()).Trim();
            return unit;
        }

        public static string Combine(IEnumerable<string> sources)
        {
            List<Unit> units = sources.Select(Split).ToList();
            if (units.Count == 0) throw new InvalidDataException("No source fragments to combine");
            List<string> packages = units.Select(u => u.PackageLine).Where(p => !String.IsNullOrEmpty(p)).Distinct().ToList();
            if (packages.Count > 1) throw new InvalidDataException("TUFA container contains classes from different packages: " + String.Join(", ", packages.ToArray()));
            List<string> imports = units.SelectMany(u => u.Imports).Distinct().ToList();
            List<string> bodies = units.Select(u => u.Body).Where(b => !String.IsNullOrEmpty(b)).ToList();
            List<string> sections = new List<string>();
            if (packages.Count > 0) sections.Add(packages[0]);
            if (imports.Count > 0) sections.Add(String.Join("\n", imports.ToArray()));
            if (bodies.Count > 0) sections.Add(String.Join("\n\n", bodies.ToArray()));
            return String.Join("\n\n", sections.ToArray()).TrimEnd('\n') + "\n";
        }
    }

    internal static class SourceFormatter
    {
        private static string ShortestFloat32(string token)
        {
            double value;
            if (!Double.TryParse(token, NumberStyles.Float, CultureInfo.InvariantCulture, out value) || Double.IsNaN(value) || Double.IsInfinity(value)) return token;
            float target = (float)value;
            int targetBits = BitConverter.ToInt32(BitConverter.GetBytes(target), 0);
            for (int precision = 1; precision <= 9; precision++)
            {
                string candidateText = value.ToString("G" + precision, CultureInfo.InvariantCulture);
                double candidate;
                if (!Double.TryParse(candidateText, NumberStyles.Float, CultureInfo.InvariantCulture, out candidate)) continue;
                float rounded = (float)candidate;
                if (BitConverter.ToInt32(BitConverter.GetBytes(rounded), 0) != targetBits) continue;
                string result = candidate.ToString("R", CultureInfo.InvariantCulture);
                if (result.IndexOf('.') < 0 && result.IndexOf('e') < 0 && result.IndexOf('E') < 0) result += ".0";
                return result;
            }
            return token;
        }

        private static string NormalizeFloatLiterals(string source)
        {
            StringBuilder output = new StringBuilder();
            int i = 0; string state = "code";
            while (i < source.Length)
            {
                char ch = source[i]; char next = i + 1 < source.Length ? source[i + 1] : '\0';
                if (state == "string" || state == "char")
                {
                    output.Append(ch);
                    if (ch == '\\' && i + 1 < source.Length) { output.Append(source[i + 1]); i += 2; continue; }
                    if ((state == "string" && ch == '"') || (state == "char" && ch == '\'')) state = "code";
                    i++; continue;
                }
                if (state == "line-comment") { output.Append(ch); if (ch == '\n') state = "code"; i++; continue; }
                if (state == "block-comment")
                {
                    output.Append(ch);
                    if (ch == '*' && next == '/') { output.Append(next); i += 2; state = "code"; } else i++;
                    continue;
                }
                if (ch == '"') { state = "string"; output.Append(ch); i++; continue; }
                if (ch == '\'') { state = "char"; output.Append(ch); i++; continue; }
                if (ch == '/' && next == '/') { state = "line-comment"; output.Append("//"); i += 2; continue; }
                if (ch == '/' && next == '*') { state = "block-comment"; output.Append("/*"); i += 2; continue; }
                char previous = i > 0 ? source[i - 1] : '\0';
                bool previousIdentifier = previous != '\0' && (Char.IsLetterOrDigit(previous) || previous == '_' || previous == '$');
                if (!previousIdentifier && (Char.IsDigit(ch) || (ch == '.' && Char.IsDigit(next))))
                {
                    Match match = Regex.Match(source.Substring(i), "^(?:(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?|\\d+[eE][+-]?\\d+)");
                    if (match.Success)
                    {
                        char after = i + match.Length < source.Length ? source[i + match.Length] : '\0';
                        bool afterIdentifier = after != '\0' && (Char.IsLetterOrDigit(after) || after == '_' || after == '$');
                        if (!afterIdentifier) { output.Append(ShortestFloat32(match.Value)); i += match.Length; continue; }
                    }
                }
                output.Append(ch); i++;
            }
            return output.ToString();
        }

        private static HashSet<string> InstanceFields(TufaClass cls)
        {
            HashSet<string> names = new HashSet<string>();
            foreach (TufaMember field in cls.InstanceFields) { try { names.Add(cls.Utf8(field.Name)); } catch { } }
            HashSet<string> staticNames = new HashSet<string>();
            foreach (TufaMember field in cls.StaticFields) { try { staticNames.Add(cls.Utf8(field.Name)); } catch { } }
            for (int i = 0; i < cls.Constants.Count; i++)
            {
                if (cls.Constants[i].Kind != "reference") continue;
                try
                {
                    TufaReference reference = cls.Reference((uint)i);
                    if (reference.Owner == cls.Name && !reference.Descriptor.StartsWith("(", StringComparison.Ordinal) && !staticNames.Contains(reference.Name)) names.Add(reference.Name);
                }
                catch { }
            }
            return names;
        }

        private static string RemoveRedundantThis(string source, TufaClass cls)
        {
            HashSet<string> fields = InstanceFields(cls);
            if (fields.Count == 0) return source;
            HashSet<string> declared = new HashSet<string>();
            foreach (Match match in Regex.Matches(source, "\\b(?:arg\\d+|local\\d+(?:_\\d+)?)\\b")) declared.Add(match.Value);
            HashSet<string> removable = new HashSet<string>(fields.Where(name => !declared.Contains(name)));
            if (removable.Count == 0) return source;
            StringBuilder output = new StringBuilder(); int i = 0; string state = "code";
            while (i < source.Length)
            {
                char ch = source[i]; char next = i + 1 < source.Length ? source[i + 1] : '\0';
                if (state == "string" || state == "char")
                {
                    output.Append(ch);
                    if (ch == '\\' && i + 1 < source.Length) { output.Append(source[i + 1]); i += 2; continue; }
                    if ((state == "string" && ch == '"') || (state == "char" && ch == '\'')) state = "code";
                    i++; continue;
                }
                if (state == "line-comment") { output.Append(ch); if (ch == '\n') state = "code"; i++; continue; }
                if (state == "block-comment")
                {
                    output.Append(ch);
                    if (ch == '*' && next == '/') { output.Append(next); i += 2; state = "code"; } else i++;
                    continue;
                }
                if (ch == '"') { state = "string"; output.Append(ch); i++; continue; }
                if (ch == '\'') { state = "char"; output.Append(ch); i++; continue; }
                if (ch == '/' && next == '/') { state = "line-comment"; output.Append("//"); i += 2; continue; }
                if (ch == '/' && next == '*') { state = "block-comment"; output.Append("/*"); i += 2; continue; }
                if (source.Substring(i).StartsWith("this.", StringComparison.Ordinal))
                {
                    Match nameMatch = Regex.Match(source.Substring(i + 5), "^[A-Za-z_$][\\w$]*");
                    if (nameMatch.Success && removable.Contains(nameMatch.Value))
                    {
                        output.Append(nameMatch.Value); i += 5 + nameMatch.Length; continue;
                    }
                }
                output.Append(ch); i++;
            }
            return output.ToString();
        }

        private static List<string> SplitArguments(string text)
        {
            List<string> args = new List<string>(); int start = 0, parens = 0, brackets = 0, braces = 0;
            char quote = '\0'; bool escaped = false;
            for (int i = 0; i < text.Length; i++)
            {
                char ch = text[i];
                if (quote != '\0')
                {
                    if (escaped) escaped = false; else if (ch == '\\') escaped = true; else if (ch == quote) quote = '\0';
                    continue;
                }
                if (ch == '"' || ch == '\'') { quote = ch; continue; }
                if (ch == '(') parens++; else if (ch == ')') parens--;
                else if (ch == '[') brackets++; else if (ch == ']') brackets--;
                else if (ch == '{') braces++; else if (ch == '}') braces--;
                else if (ch == ',' && parens == 0 && brackets == 0 && braces == 0) { args.Add(text.Substring(start, i - start).Trim()); start = i + 1; }
            }
            string tail = text.Substring(start).Trim(); if (tail.Length > 0) args.Add(tail);
            return args;
        }

        private static string WrapLongCalls(string source, int maxLineLength)
        {
            List<string> result = new List<string>();
            foreach (string line in source.Split('\n'))
            {
                if (line.Length <= maxLineLength || !line.TrimEnd().EndsWith(");", StringComparison.Ordinal) || line.TrimStart().StartsWith("//", StringComparison.Ordinal)) { result.Add(line); continue; }
                Match leadingMatch = Regex.Match(line, "^\\s*"); string leading = leadingMatch.Value;
                string content = line.Substring(leading.Length).TrimEnd(); int open = -1; char quote = '\0'; bool escaped = false;
                for (int i = 0; i < content.Length; i++)
                {
                    char ch = content[i];
                    if (quote != '\0') { if (escaped) escaped = false; else if (ch == '\\') escaped = true; else if (ch == quote) quote = '\0'; }
                    else if (ch == '"' || ch == '\'') quote = ch; else if (ch == '(') { open = i; break; }
                }
                if (open < 1 || !content.EndsWith(");", StringComparison.Ordinal)) { result.Add(line); continue; }
                List<string> args = SplitArguments(content.Substring(open + 1, content.Length - open - 3));
                if (args.Count < 2) { result.Add(line); continue; }
                result.Add(leading + content.Substring(0, open + 1));
                for (int i = 0; i < args.Count; i++) result.Add(leading + "    " + args[i] + (i == args.Count - 1 ? "" : ","));
                result.Add(leading + ");");
            }
            return String.Join("\n", result.ToArray());
        }

        private static string BraceAndIndentStyle(string source)
        {
            List<string> result = new List<string>();
            foreach (string original in source.Split('\n'))
            {
                string line = Regex.Replace(original, "^(?: {4})+", delegate(Match m) { return new String('\t', m.Length / 4); });
                string trimmed = line.Trim();
                if (trimmed.Length == 0 || trimmed.StartsWith("//", StringComparison.Ordinal) || trimmed.StartsWith("/*", StringComparison.Ordinal) || trimmed.StartsWith("*", StringComparison.Ordinal)) { result.Add(line); continue; }
                Match match = Regex.Match(line, "^(\\s*)(.*\\S)\\s+\\{$");
                if (match.Success) { result.Add(match.Groups[1].Value + match.Groups[2].Value); result.Add(match.Groups[1].Value + "{"); }
                else result.Add(line);
            }
            return String.Join("\n", result.ToArray());
        }

        public static string Format(string source, TufaClass cls)
        {
            string result = NormalizeFloatLiterals(source);
            result = RemoveRedundantThis(result, cls);
            result = WrapLongCalls(result, 112);
            result = BraceAndIndentStyle(result);
            return result;
        }
    }

    internal static class DecompilerService
    {
        public static FileDecompileResult ProcessFile(string inputPath)
        {
            FileDecompileResult fileResult = new FileDecompileResult { InputPath = Path.GetFullPath(inputPath) };
            Stopwatch total = Stopwatch.StartNew();
            try
            {
                if (!fileResult.InputPath.EndsWith(".class", StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException("Only .class files are supported");
                if (!File.Exists(fileResult.InputPath)) throw new FileNotFoundException("The input file does not exist", fileResult.InputPath);

                Stopwatch stage = Stopwatch.StartNew();
                byte[] bytes;
                stage.Restart();
                try { bytes = File.ReadAllBytes(fileResult.InputPath); }
                finally
                {
                    stage.Stop();
                    fileResult.Timings.ReadMs = stage.Elapsed.TotalMilliseconds;
                }

                List<TufaClass> classes;
                stage.Restart();
                try { classes = TufaParser.Parse(bytes); }
                finally
                {
                    stage.Stop();
                    fileResult.Timings.ParseMs = stage.Elapsed.TotalMilliseconds;
                }
                if (classes.Count == 0) throw new InvalidDataException("No TUFA classes were found in the file");

                string stem = Path.GetFileNameWithoutExtension(fileResult.InputPath);
                List<string> sources = new List<string>(classes.Count);
                foreach (TufaClass cls in classes)
                {
                    if (String.IsNullOrEmpty(cls.Name))
                    {
                        string inferred = Regex.Replace(stem, "[^A-Za-z0-9_$]", "_");
                        if (!Regex.IsMatch(inferred, "^[A-Za-z_$]")) inferred = "_" + inferred;
                        if (TufaParser.RecoverMissingClassName(cls, inferred)) fileResult.UsedFilenameFallback = true;
                    }

                    DecompileResult result;
                    stage.Restart();
                    try { result = DecompileEngine.DecompileClass(cls); }
                    finally
                    {
                        stage.Stop();
                        fileResult.Timings.DecompileMs += stage.Elapsed.TotalMilliseconds;
                    }

                    stage.Restart();
                    try { sources.Add(SourceFormatter.Format(result.Source, cls)); }
                    finally
                    {
                        stage.Stop();
                        fileResult.Timings.FormatMs += stage.Elapsed.TotalMilliseconds;
                    }

                    fileResult.ClassNames.Add(cls.Name);
                    fileResult.Diagnostics.AddRange(result.Diagnostics.Select(message => cls.Name + ": " + message));
                }

                string source;
                stage.Restart();
                try { source = SourceBundle.Combine(sources); }
                finally
                {
                    stage.Stop();
                    fileResult.Timings.CombineMs = stage.Elapsed.TotalMilliseconds;
                }

                string outputPath;
                stage.Restart();
                try { outputPath = WriteUniqueOutput(Path.GetDirectoryName(fileResult.InputPath), stem, source); }
                finally
                {
                    stage.Stop();
                    fileResult.Timings.WriteMs = stage.Elapsed.TotalMilliseconds;
                }

                fileResult.OutputPath = outputPath;
                fileResult.Source = source;
                fileResult.ClassCount = classes.Count;
                fileResult.ClassName = classes.Count == 1 ? classes[0].Name : stem;
            }
            catch (Exception error)
            {
                fileResult.Error = error.Message;
            }
            finally
            {
                total.Stop();
                fileResult.Timings.TotalMs = total.Elapsed.TotalMilliseconds;
            }
            return fileResult;
        }

        public static int ProcessBatch(IEnumerable<string> paths)
        {
            int failures = 0;
            foreach (string path in paths)
            {
                FileDecompileResult result = ProcessFile(path);
                if (!result.Success) failures++;
            }
            return failures == 0 ? 0 : 1;
        }

        private static string WriteUniqueOutput(string directory, string stem, string source)
        {
            for (int suffix = 0; suffix < 10000; suffix++)
            {
                string fileName = suffix == 0 ? stem + ".java" : suffix == 1 ? stem + ".decompiled.java" : stem + ".decompiled (" + suffix + ").java";
                string candidate = Path.Combine(directory, fileName);
                try
                {
                    using (FileStream stream = new FileStream(candidate, FileMode.CreateNew, FileAccess.Write, FileShare.None))
                    using (StreamWriter writer = new StreamWriter(stream, new UTF8Encoding(false)))
                    {
                        writer.Write(source);
                    }
                    return candidate;
                }
                catch (IOException)
                {
                    // CreateNew is atomic: if another process created this path,
                    // keep the existing file and try the next safe name.
                    if (File.Exists(candidate)) continue;
                    throw;
                }
            }
            throw new IOException("Could not find an available output name for " + stem + ".java");
        }
    }
}
