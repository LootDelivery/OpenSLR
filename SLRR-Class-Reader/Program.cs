using System;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace SLRRClassReader
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            string[] classPaths = (args ?? new string[0])
                .Where(path => !String.IsNullOrWhiteSpace(path) && path.Trim('"').EndsWith(".class", StringComparison.OrdinalIgnoreCase))
                .Select(path => Path.GetFullPath(path.Trim('"')))
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToArray();

            if (classPaths.Length > 0)
            {
                // Dropping .class files directly onto the EXE runs a silent batch.
                return DecompilerService.ProcessBatch(classPaths);
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
            return 0;
        }
    }
}
