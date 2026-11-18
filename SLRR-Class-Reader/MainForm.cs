using System;
using System.Collections.Generic;
using System.Drawing;
using System.Diagnostics;
using System.Drawing.Drawing2D;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace SLRRClassReader
{
    internal sealed class MainForm : Form
    {
        private const int WM_HSCROLL = 0x0114;
        private const int SB_LEFT = 6;

        [DllImport("user32.dll", CharSet = CharSet.Auto)]
        private static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);

        private void ResetSourceHorizontalScroll()
        {
            if (sourceBox == null || !sourceBox.IsHandleCreated) return;
            SendMessage(sourceBox.Handle, WM_HSCROLL, new IntPtr(SB_LEFT), IntPtr.Zero);
        }
        private sealed class ResultEntry
        {
            public FileDecompileResult Result;
            public string DisplayName;
            public override string ToString() { return DisplayName; }
        }

        private readonly Color background = Color.FromArgb(16, 19, 24);
        private readonly Color panel = Color.FromArgb(20, 24, 30);
        private readonly Color panelAlt = Color.FromArgb(29, 34, 43);
        private readonly Color panelHover = Color.FromArgb(37, 44, 55);
        private readonly Color foreground = Color.FromArgb(230, 233, 238);
        private readonly Color muted = Color.FromArgb(137, 147, 162);
        private readonly Color dim = Color.FromArgb(96, 107, 122);
        private readonly Color accent = Color.FromArgb(213, 164, 91);
        private readonly Color accentHover = Color.FromArgb(227, 179, 108);
        private readonly Color border = Color.FromArgb(42, 48, 58);
        private readonly Color borderSoft = Color.FromArgb(34, 40, 50);
        private readonly Color success = Color.FromArgb(121, 184, 138);
        private readonly Color warning = Color.FromArgb(213, 164, 91);
        private readonly Color danger = Color.FromArgb(223, 119, 119);
        private readonly List<ResultEntry> entries = new List<ResultEntry>();
        private readonly ListBox resultList = new ListBox();
        private readonly SmoothScrollingRichTextBox sourceBox = new SmoothScrollingRichTextBox();
        private readonly LineNumberGutter lineNumberGutter;
        private readonly RichTextBox diagnosticsBox = new RichTextBox();
        private readonly Label titleLabel = new Label();
        private readonly Label fileLabel = new Label();
        private readonly Label performanceLabel = new Label();
        private readonly Label outputLabel = new Label();
        private readonly Label lineLabel = new Label();
        private readonly Label statusLabel = new Label();
        private readonly Label resultCountLabel = new Label();
        private readonly Label resultsHeaderLabel = new Label();
        private readonly Label welcomeLabel = new Label();
        private readonly Label welcomeDescriptionLabel = new Label();
        private readonly Label welcomeSymbolLabel = new Label();
        private readonly Label welcomeFootnoteLabel = new Label();
        private readonly Label dropTitleLabel = new Label();
        private readonly Label dropSubtitleLabel = new Label();
        private readonly Label emptyResultsLabel = new Label();
        private readonly Label fileBadgeLabel = new Label();
        private readonly Label statusIndicatorLabel = new Label();
        private readonly Label statusFormatLabel = new Label();
        private readonly Label diagnosticsHeader = new Label();
        private readonly Button openButton = new Button();
        private readonly Button copyButton = new Button();
        private readonly Button revealButton = new Button();
        private readonly Button welcomeOpenButton = new Button();
        private readonly ComboBox languageBox = new ComboBox();
        private readonly ComboBox scrollSpeedBox = new ComboBox();
        private readonly Label languageLabel = new Label();
        private readonly Label scrollSpeedLabel = new Label();
        private readonly Label fontSizeLabel = new Label();
        private readonly ComboBox fontSizeBox = new ComboBox();
        private readonly FlowLayoutPanel toolbarPanel = new FlowLayoutPanel();
        private readonly Panel welcomePanel = new Panel();
        private readonly Panel dropPanel = new Panel();
        private readonly Panel readerPanel = new Panel();
        private readonly TableLayoutPanel readerLayout = new TableLayoutPanel();
        private readonly Panel readerHeaderPanel = new Panel();
        private readonly Panel diagnosticsPanel = new Panel();
        private readonly Panel outputPanel = new Panel();
        private Panel leftPanel = new Panel();
        private Panel rightPanel = new Panel();
        private Panel statusPanel = new Panel();
        private string languagePreference = "system";
        private string currentLanguage = "en";
        private int scrollSpeedIndex;
        private int fontSizeIndex = 1;
        // Font instances are cached for the form lifetime. WinForms may keep a
        // reference to an equal Font instance when Control.Font is assigned.
        private readonly Font[] sourceViewerFonts = new Font[]
        {
            new Font("Consolas", 8.0F, FontStyle.Regular, GraphicsUnit.Point),
            new Font("Consolas", 9.5F, FontStyle.Regular, GraphicsUnit.Point),
            new Font("Consolas", 11.0F, FontStyle.Regular, GraphicsUnit.Point),
            new Font("Consolas", 12.5F, FontStyle.Regular, GraphicsUnit.Point),
            new Font("Consolas", 14.0F, FontStyle.Regular, GraphicsUnit.Point)
        };
        private bool hasBatchSummary;
        private int lastBatchSucceeded;
        private int lastBatchFailed;
        private double lastBatchEngineMs;
        private double lastBatchWallMs;
        private bool busy;
        private int hoveredResultIndex = -1;

        private string R(string ru, string en) { return currentLanguage == "ru" ? ru : en; }

        public MainForm()
        {
            Text = "SLRR Class Reader";
            StartPosition = FormStartPosition.CenterScreen;
            MinimumSize = new Size(940, 640);
            Size = new Size(1220, 820);
            BackColor = background;
            ForeColor = foreground;
            Font = new Font("Segoe UI", 9F, FontStyle.Regular, GraphicsUnit.Point);
            lineNumberGutter = new LineNumberGutter(sourceBox);
            sourceBox.ViewportChanged += delegate { lineNumberGutter.Invalidate(); };
            BuildLayout();
            ConfigureDropTarget(this);
            ConfigureDropTarget(leftPanel);
            ConfigureDropTarget(rightPanel);
            ConfigureDropTarget(welcomePanel);
            ConfigureDropTarget(readerPanel);
            ConfigureDropTarget(sourceBox);
            ConfigureDropTarget(resultList);
            Load += OnLoad;
            FormClosing += OnFormClosing;
        }

        private void BuildLayout()
        {
            Panel top = new Panel { Dock = DockStyle.Top, Height = 72, BackColor = panel, Padding = new Padding(18, 10, 18, 10) };
            top.Paint += delegate(object sender, PaintEventArgs e)
            {
                using (Pen pen = new Pen(borderSoft)) e.Graphics.DrawLine(pen, 0, top.Height - 1, top.Width, top.Height - 1);
            };
            Controls.Add(top);
            ConfigureDropTarget(top);

            Label mark = new Label { Text = "TU", TextAlign = ContentAlignment.MiddleCenter, Font = new Font("Segoe UI", 11F, FontStyle.Bold), ForeColor = accent, BackColor = Color.FromArgb(40, 35, 28), BorderStyle = BorderStyle.FixedSingle, Bounds = new Rectangle(18, 16, 40, 40) };
            top.Controls.Add(mark);
            Label brand = new Label { Text = "SLRR CLASS READER", Font = new Font("Segoe UI", 10F, FontStyle.Bold), ForeColor = foreground, AutoSize = true, Location = new Point(72, 18) };
            top.Controls.Add(brand);
            Label caption = new Label { Text = "TUFA / TREE DECOMPILER", Font = new Font("Segoe UI", 8F), ForeColor = muted, AutoSize = true, Location = new Point(72, 40) };
            top.Controls.Add(caption);

            Panel languageGroup = new Panel { Width = 112, Height = 48, Margin = new Padding(0, 0, 8, 0), BackColor = Color.Transparent };
            languageLabel.Text = "LANGUAGE";
            languageLabel.Font = new Font("Segoe UI", 7.5F, FontStyle.Bold);
            languageLabel.ForeColor = muted;
            languageLabel.Location = new Point(1, 0);
            languageLabel.Size = new Size(110, 14);
            languageGroup.Controls.Add(languageLabel);

            languageBox.DropDownStyle = ComboBoxStyle.DropDownList;
            languageBox.FlatStyle = FlatStyle.Flat;
            languageBox.BackColor = panelAlt;
            languageBox.ForeColor = foreground;
            languageBox.Font = new Font("Segoe UI", 8.5F);
            languageBox.Bounds = new Rectangle(0, 16, 112, 27);
            languageBox.Items.AddRange(new object[] { "Auto", "Русский", "English" });
            languageBox.SelectedIndexChanged += OnLanguageChanged;
            languageGroup.Controls.Add(languageBox);

            Panel scrollSpeedGroup = new Panel { Width = 122, Height = 48, Margin = new Padding(0, 0, 8, 0), BackColor = Color.Transparent };
            scrollSpeedLabel.Text = "SCROLL SPEED";
            scrollSpeedLabel.Font = new Font("Segoe UI", 7.5F, FontStyle.Bold);
            scrollSpeedLabel.ForeColor = muted;
            scrollSpeedLabel.Location = new Point(1, 0);
            scrollSpeedLabel.Size = new Size(120, 14);
            scrollSpeedGroup.Controls.Add(scrollSpeedLabel);

            scrollSpeedBox.DropDownStyle = ComboBoxStyle.DropDownList;
            scrollSpeedBox.FlatStyle = FlatStyle.Flat;
            scrollSpeedBox.BackColor = panelAlt;
            scrollSpeedBox.ForeColor = foreground;
            scrollSpeedBox.Font = new Font("Segoe UI", 8.5F);
            scrollSpeedBox.Bounds = new Rectangle(0, 16, 122, 27);
            scrollSpeedBox.Items.AddRange(new object[] { "×1", "×1.5", "×2", "×3" });
            scrollSpeedBox.SelectedIndexChanged += OnScrollSpeedChanged;
            scrollSpeedGroup.Controls.Add(scrollSpeedBox);

            toolbarPanel.FlowDirection = FlowDirection.LeftToRight;
            toolbarPanel.WrapContents = false;
            toolbarPanel.AutoSize = false;
            toolbarPanel.Padding = Padding.Empty;
            toolbarPanel.Margin = Padding.Empty;
            toolbarPanel.Size = new Size(648, 48);
            toolbarPanel.Location = new Point(top.ClientSize.Width - toolbarPanel.Width - 18, 12);
            toolbarPanel.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            toolbarPanel.Controls.Add(languageGroup);
            toolbarPanel.Controls.Add(scrollSpeedGroup);

            openButton.Size = new Size(118, 36);
            openButton.Margin = new Padding(0, 6, 8, 0);
            openButton.Click += async delegate { await OpenDialog(); };
            StyleButton(openButton, false);
            toolbarPanel.Controls.Add(openButton);

            revealButton.Size = new Size(132, 36);
            revealButton.Margin = new Padding(0, 6, 8, 0);
            revealButton.Click += RevealSelected;
            StyleButton(revealButton, false);
            toolbarPanel.Controls.Add(revealButton);

            copyButton.Size = new Size(132, 36);
            copyButton.Margin = new Padding(0, 6, 0, 0);
            copyButton.Click += CopySelected;
            StyleButton(copyButton, true);
            toolbarPanel.Controls.Add(copyButton);

            top.Controls.Add(toolbarPanel);

            Panel workspace = new Panel { Dock = DockStyle.Fill, BackColor = background, Padding = new Padding(0) };
            Controls.Add(workspace);
            workspace.BringToFront();

            leftPanel = new Panel { Dock = DockStyle.Left, Width = 272, BackColor = panel, Padding = new Padding(14, 15, 14, 12) };
            workspace.Controls.Add(leftPanel);
            Panel separator = new Panel { Dock = DockStyle.Left, Width = 1, BackColor = border };
            workspace.Controls.Add(separator);
            rightPanel = new Panel { Dock = DockStyle.Fill, BackColor = background, Padding = new Padding(24, 18, 24, 16) };
            workspace.Controls.Add(rightPanel);
            ConfigureDropTarget(workspace);
            // Docking is processed from the last control in Z-order to the first.
            // Reserve the left navigation and separator before filling the remaining area.
            workspace.Controls.SetChildIndex(rightPanel, 0);
            workspace.Controls.SetChildIndex(separator, 1);
            workspace.Controls.SetChildIndex(leftPanel, 2);

            resultsHeaderLabel.Text = "РЕЗУЛЬТАТЫ";
            resultsHeaderLabel.ForeColor = muted;
            resultsHeaderLabel.Font = new Font("Segoe UI", 8F, FontStyle.Bold);
            resultsHeaderLabel.AutoSize = true;
            resultsHeaderLabel.Location = new Point(14, 17);
            leftPanel.Controls.Add(resultsHeaderLabel);
            resultCountLabel.Text = "0";
            resultCountLabel.ForeColor = muted;
            resultCountLabel.AutoSize = false;
            resultCountLabel.TextAlign = ContentAlignment.MiddleCenter;
            resultCountLabel.Size = new Size(24, 20);
            resultCountLabel.Location = new Point(206, 14);
            resultCountLabel.BackColor = panelAlt;
            leftPanel.Controls.Add(resultCountLabel);

            resultList.DrawMode = DrawMode.OwnerDrawFixed;
            resultList.ItemHeight = 62;
            resultList.BorderStyle = BorderStyle.None;
            resultList.BackColor = panel;
            resultList.ForeColor = foreground;
            resultList.Font = new Font("Segoe UI", 9F);
            resultList.Location = new Point(8, 52);
            resultList.Size = new Size(256, 620);
            resultList.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            resultList.IntegralHeight = false;
            resultList.DrawItem += DrawResultItem;
            resultList.SelectedIndexChanged += OnSelectionChanged;
            resultList.MouseMove += OnResultListMouseMove;
            resultList.MouseLeave += OnResultListMouseLeave;
            leftPanel.Controls.Add(resultList);

            emptyResultsLabel.Text = "No files yet";
            emptyResultsLabel.Font = new Font("Segoe UI", 9F);
            emptyResultsLabel.ForeColor = dim;
            emptyResultsLabel.TextAlign = ContentAlignment.MiddleCenter;
            emptyResultsLabel.Location = new Point(18, 130);
            emptyResultsLabel.Size = new Size(236, 70);
            emptyResultsLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            emptyResultsLabel.BackColor = panel;
            emptyResultsLabel.Visible = true;
            leftPanel.Controls.Add(emptyResultsLabel);
            ConfigureDropTarget(emptyResultsLabel);

            Label engineLabel = new Label { Text = "●  TUFA / TREE     2.2.3", ForeColor = dim, AutoSize = true, Anchor = AnchorStyles.Bottom | AnchorStyles.Left, Location = new Point(14, 680) };
            leftPanel.Controls.Add(engineLabel);
            leftPanel.Resize += delegate
            {
                engineLabel.Location = new Point(14, leftPanel.ClientSize.Height - 28);
                resultList.Bounds = new Rectangle(8, 52, Math.Max(80, leftPanel.ClientSize.Width - 16), Math.Max(80, leftPanel.ClientSize.Height - 94));
                emptyResultsLabel.Width = Math.Max(80, leftPanel.ClientSize.Width - 36);
            };

            welcomePanel.Dock = DockStyle.Fill;
            welcomePanel.BackColor = background;

            welcomeSymbolLabel.Text = ".class     →     .java";
            welcomeSymbolLabel.Font = new Font("Consolas", 16F, FontStyle.Bold);
            welcomeSymbolLabel.ForeColor = accent;
            welcomeSymbolLabel.TextAlign = ContentAlignment.MiddleCenter;
            welcomePanel.Controls.Add(welcomeSymbolLabel);

            welcomeLabel.Text = "SLRR Class Reader";
            welcomeLabel.Font = new Font("Segoe UI", 22F, FontStyle.Bold);
            welcomeLabel.ForeColor = foreground;
            welcomeLabel.TextAlign = ContentAlignment.MiddleCenter;
            welcomePanel.Controls.Add(welcomeLabel);

            welcomeDescriptionLabel.Text = "Readable Java-like source reconstructed from SLRR class files.";
            welcomeDescriptionLabel.ForeColor = muted;
            welcomeDescriptionLabel.Font = new Font("Segoe UI", 9.5F);
            welcomeDescriptionLabel.TextAlign = ContentAlignment.MiddleCenter;
            welcomePanel.Controls.Add(welcomeDescriptionLabel);

            dropPanel.BackColor = panel;
            dropPanel.Paint += PaintDropPanel;
            welcomePanel.Controls.Add(dropPanel);

            dropTitleLabel.Text = "DROP .CLASS FILES HERE";
            dropTitleLabel.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            dropTitleLabel.ForeColor = foreground;
            dropTitleLabel.TextAlign = ContentAlignment.MiddleCenter;
            dropPanel.Controls.Add(dropTitleLabel);

            dropSubtitleLabel.Text = "or choose files manually";
            dropSubtitleLabel.Font = new Font("Segoe UI", 8.5F);
            dropSubtitleLabel.ForeColor = dim;
            dropSubtitleLabel.TextAlign = ContentAlignment.MiddleCenter;
            dropPanel.Controls.Add(dropSubtitleLabel);

            welcomeOpenButton.Text = "Choose .class files";
            welcomeOpenButton.Size = new Size(190, 36);
            welcomeOpenButton.Click += async delegate { await OpenDialog(); };
            StyleButton(welcomeOpenButton, true);
            dropPanel.Controls.Add(welcomeOpenButton);

            welcomeFootnoteLabel.Text = "INPUT FILES ARE NEVER MODIFIED  ·  JAVA IS SAVED BESIDE THE CLASS";
            welcomeFootnoteLabel.Font = new Font("Segoe UI", 8F);
            welcomeFootnoteLabel.ForeColor = dim;
            welcomeFootnoteLabel.TextAlign = ContentAlignment.MiddleCenter;
            welcomePanel.Controls.Add(welcomeFootnoteLabel);

            ConfigureDropTarget(dropPanel);
            ConfigureDropTarget(dropTitleLabel);
            ConfigureDropTarget(dropSubtitleLabel);
            ConfigureDropTarget(welcomeOpenButton);
            welcomePanel.Resize += delegate { LayoutWelcomePanel(); };
            LayoutWelcomePanel();

            readerPanel.Dock = DockStyle.Fill;
            readerPanel.BackColor = background;
            readerPanel.Visible = false;
            rightPanel.Controls.Add(readerPanel);
            rightPanel.Controls.Add(welcomePanel);

            readerLayout.Dock = DockStyle.Fill;
            readerLayout.BackColor = background;
            readerLayout.Margin = Padding.Empty;
            readerLayout.Padding = Padding.Empty;
            readerLayout.ColumnCount = 1;
            readerLayout.RowCount = 4;
            readerLayout.GrowStyle = TableLayoutPanelGrowStyle.FixedSize;
            readerLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100F));
            readerLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 76F));
            readerLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 0F));
            readerLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100F));
            readerLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 42F));

            readerHeaderPanel.Dock = DockStyle.Fill;
            readerHeaderPanel.Margin = Padding.Empty;
            readerHeaderPanel.BackColor = background;

            readerHeaderPanel.Paint += delegate(object sender, PaintEventArgs e)
            {
                using (Pen pen = new Pen(borderSoft)) e.Graphics.DrawLine(pen, 0, readerHeaderPanel.Height - 1, readerHeaderPanel.Width, readerHeaderPanel.Height - 1);
            };

            fileBadgeLabel.Text = "JAVA";
            fileBadgeLabel.Font = new Font("Consolas", 8.5F, FontStyle.Bold);
            fileBadgeLabel.ForeColor = accent;
            fileBadgeLabel.BackColor = Color.FromArgb(40, 35, 28);
            fileBadgeLabel.BorderStyle = BorderStyle.FixedSingle;
            fileBadgeLabel.TextAlign = ContentAlignment.MiddleCenter;
            fileBadgeLabel.Bounds = new Rectangle(0, 14, 42, 38);
            readerHeaderPanel.Controls.Add(fileBadgeLabel);

            titleLabel.Text = "ClassName";
            titleLabel.Font = new Font("Segoe UI", 13F, FontStyle.Bold);
            titleLabel.ForeColor = foreground;
            titleLabel.AutoSize = false;
            titleLabel.Height = 25;
            titleLabel.TextAlign = ContentAlignment.MiddleLeft;
            titleLabel.AutoEllipsis = true;
            titleLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            titleLabel.Location = new Point(56, 4);
            readerHeaderPanel.Controls.Add(titleLabel);

            fileLabel.Text = "source.java";
            fileLabel.Font = new Font("Segoe UI", 9F);
            fileLabel.ForeColor = muted;
            fileLabel.AutoSize = false;
            fileLabel.Height = 18;
            fileLabel.TextAlign = ContentAlignment.MiddleLeft;
            fileLabel.AutoEllipsis = true;
            fileLabel.Location = new Point(56, 29);
            readerHeaderPanel.Controls.Add(fileLabel);

            performanceLabel.Text = "";
            performanceLabel.Font = new Font("Segoe UI", 8F);
            performanceLabel.ForeColor = muted;
            performanceLabel.AutoSize = false;
            performanceLabel.Location = new Point(56, 49);
            performanceLabel.Height = 18;
            performanceLabel.AutoEllipsis = true;
            performanceLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            readerHeaderPanel.Controls.Add(performanceLabel);

            lineLabel.Text = "0 lines";
            lineLabel.Font = new Font("Segoe UI", 8.5F);
            lineLabel.ForeColor = muted;
            lineLabel.AutoSize = true;
            lineLabel.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            lineLabel.Location = new Point(800, 9);
            readerHeaderPanel.Controls.Add(lineLabel);

            diagnosticsPanel.Dock = DockStyle.Fill;
            diagnosticsPanel.Margin = Padding.Empty;
            diagnosticsPanel.BackColor = background;
            diagnosticsPanel.Visible = false;

            diagnosticsHeader.Text = "DIAGNOSTICS";
            diagnosticsHeader.Font = new Font("Segoe UI", 8F, FontStyle.Bold);
            diagnosticsHeader.ForeColor = muted;
            diagnosticsHeader.AutoSize = true;
            diagnosticsHeader.Location = new Point(0, 0);
            diagnosticsPanel.Controls.Add(diagnosticsHeader);

            diagnosticsBox.ReadOnly = true;
            diagnosticsBox.BorderStyle = BorderStyle.FixedSingle;
            diagnosticsBox.BackColor = Color.FromArgb(38, 34, 27);
            diagnosticsBox.ForeColor = Color.FromArgb(225, 190, 132);
            diagnosticsBox.Font = new Font("Consolas", 8.5F);
            diagnosticsBox.Location = new Point(0, 20);
            diagnosticsBox.Size = new Size(850, 64);
            diagnosticsBox.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            diagnosticsBox.ScrollBars = RichTextBoxScrollBars.Both;
            diagnosticsBox.Visible = true;
            diagnosticsPanel.Controls.Add(diagnosticsBox);

            sourceBox.ReadOnly = true;
            sourceBox.MaxLength = Int32.MaxValue;
            sourceBox.DetectUrls = false;
            sourceBox.BorderStyle = BorderStyle.FixedSingle;
            sourceBox.BackColor = Color.FromArgb(15, 18, 23);
            sourceBox.ForeColor = Color.FromArgb(213, 219, 229);
            sourceBox.Font = sourceViewerFonts[fontSizeIndex];
            sourceBox.WordWrap = false;
            sourceBox.ScrollBars = RichTextBoxScrollBars.Both;
            sourceBox.AcceptsTab = true;
            sourceBox.Dock = DockStyle.Fill;
            sourceBox.Margin = Padding.Empty;
            sourceBox.Resize += delegate { ResetSourceHorizontalScroll(); };
            sourceBox.HandleCreated += delegate { ResetSourceHorizontalScroll(); };
            TableLayoutPanel sourceHost = new TableLayoutPanel();
            sourceHost.Dock = DockStyle.Fill;
            sourceHost.Margin = Padding.Empty;
            sourceHost.Padding = Padding.Empty;
            sourceHost.BackColor = sourceBox.BackColor;
            sourceHost.ColumnCount = 2;
            sourceHost.RowCount = 1;
            sourceHost.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 52F));
            sourceHost.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100F));
            sourceHost.RowStyles.Add(new RowStyle(SizeType.Percent, 100F));
            lineNumberGutter.Dock = DockStyle.Fill;
            lineNumberGutter.Margin = Padding.Empty;
            lineNumberGutter.BackColor = Color.FromArgb(18, 22, 28);
            sourceHost.Controls.Add(lineNumberGutter, 0, 0);
            sourceHost.Controls.Add(sourceBox, 1, 0);
            ConfigureDropTarget(sourceHost);

            outputPanel.Dock = DockStyle.Fill;
            outputPanel.Margin = Padding.Empty;
            outputPanel.BackColor = panel;
            outputPanel.Paint += delegate(object sender, PaintEventArgs e)
            {
                using (Pen pen = new Pen(borderSoft)) e.Graphics.DrawLine(pen, 0, 0, outputPanel.Width, 0);
            };

            outputLabel.Text = "SAVED TO";
            outputLabel.Font = new Font("Segoe UI", 8F, FontStyle.Bold);
            outputLabel.ForeColor = muted;
            outputLabel.AutoSize = false;
            outputLabel.AutoEllipsis = true;
            outputLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            outputLabel.Location = new Point(0, 11);
            outputLabel.Height = 18;
            outputPanel.Controls.Add(outputLabel);

            fontSizeLabel.Text = "FONT";
            fontSizeLabel.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            fontSizeLabel.ForeColor = muted;
            fontSizeLabel.TextAlign = ContentAlignment.MiddleCenter;
            fontSizeLabel.Size = new Size(48, 24);
            fontSizeLabel.AccessibleName = R("Размер шрифта", "Font size");
            outputPanel.Controls.Add(fontSizeLabel);

            fontSizeBox.DropDownStyle = ComboBoxStyle.DropDownList;
            fontSizeBox.FlatStyle = FlatStyle.Flat;
            fontSizeBox.BackColor = panelAlt;
            fontSizeBox.ForeColor = foreground;
            fontSizeBox.Font = new Font("Segoe UI", 8.5F);
            fontSizeBox.Size = new Size(86, 27);
            fontSizeBox.Items.AddRange(new object[] { "8 pt", "9.5 pt", "11 pt", "12.5 pt", "14 pt" });
            fontSizeBox.SelectedIndexChanged += OnFontSizeChanged;
            outputPanel.Controls.Add(fontSizeBox);

            readerLayout.Controls.Add(readerHeaderPanel, 0, 0);
            readerLayout.Controls.Add(diagnosticsPanel, 0, 1);
            readerLayout.Controls.Add(sourceHost, 0, 2);
            readerLayout.Controls.Add(outputPanel, 0, 3);
            readerPanel.Controls.Add(readerLayout);
            readerPanel.Resize += delegate { LayoutReaderPane(); };

            statusPanel.Dock = DockStyle.Bottom;
            statusPanel.Height = 30;
            statusPanel.BackColor = panel;
            statusPanel.Paint += delegate(object sender, PaintEventArgs e)
            {
                using (Pen pen = new Pen(border)) e.Graphics.DrawLine(pen, 0, 0, statusPanel.Width, 0);
            };
            Controls.Add(statusPanel);
            statusPanel.BringToFront();

            statusIndicatorLabel.Text = "●";
            statusIndicatorLabel.Font = new Font("Segoe UI", 8F);
            statusIndicatorLabel.ForeColor = success;
            statusIndicatorLabel.TextAlign = ContentAlignment.MiddleCenter;
            statusIndicatorLabel.Bounds = new Rectangle(12, 0, 16, 30);
            statusPanel.Controls.Add(statusIndicatorLabel);

            statusFormatLabel.Text = "TUFA / TREE";
            statusFormatLabel.Font = new Font("Segoe UI", 8F, FontStyle.Bold);
            statusFormatLabel.ForeColor = dim;
            statusFormatLabel.TextAlign = ContentAlignment.MiddleRight;
            statusFormatLabel.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            statusFormatLabel.Bounds = new Rectangle(statusPanel.ClientSize.Width - 150, 0, 134, 30);
            statusPanel.Controls.Add(statusFormatLabel);

            statusLabel.Text = "Ready";
            statusLabel.ForeColor = muted;
            statusLabel.AutoSize = false;
            statusLabel.TextAlign = ContentAlignment.MiddleLeft;
            statusLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            statusLabel.Location = new Point(32, 0);
            statusLabel.Height = 30;
            statusLabel.Width = Math.Max(100, statusPanel.ClientSize.Width - 190);
            statusPanel.Controls.Add(statusLabel);
            statusPanel.Resize += delegate
            {
                statusLabel.Width = Math.Max(100, statusPanel.ClientSize.Width - 190);
                statusFormatLabel.Left = statusPanel.ClientSize.Width - statusFormatLabel.Width - 14;
            };
            ConfigureDropTarget(statusPanel);
            // Reserve the top header and bottom status bar before filling the center.
            Controls.SetChildIndex(workspace, 0);
            Controls.SetChildIndex(statusPanel, 1);
            Controls.SetChildIndex(top, 2);

            ApplyLanguage();
            UpdateButtons();
        }

        private void StyleButton(Button button, bool primary)
        {
            button.UseVisualStyleBackColor = false;
            button.FlatStyle = FlatStyle.Flat;
            button.FlatAppearance.BorderSize = primary ? 0 : 1;
            button.FlatAppearance.BorderColor = border;
            button.FlatAppearance.MouseOverBackColor = primary ? accentHover : panelHover;
            button.FlatAppearance.MouseDownBackColor = primary ? Color.FromArgb(190, 139, 69) : Color.FromArgb(30, 36, 45);
            button.BackColor = primary ? accent : panelAlt;
            button.ForeColor = primary ? Color.FromArgb(29, 24, 17) : foreground;
            button.Font = new Font("Segoe UI", 8.5F, FontStyle.Bold);
            button.Cursor = Cursors.Hand;
        }

        private void OnLoad(object sender, EventArgs e)
        {
            LoadSettings();
            ApplyLanguage();
            languageBox.SelectedIndex = languagePreference == "ru" ? 1 : languagePreference == "en" ? 2 : 0;
            scrollSpeedBox.SelectedIndex = scrollSpeedIndex;
            fontSizeBox.SelectedIndex = fontSizeIndex;
            SetStatus(R("Готов к работе", "Ready"));
        }

        private void LoadSettings()
        {
            try
            {
                string path = SettingsPath();
                if (File.Exists(path))
                {
                    string value = File.ReadAllText(path).Trim().ToLowerInvariant();
                    if (value == "ru" || value == "en" || value == "system") languagePreference = value;
                }
            }
            catch { languagePreference = "system"; }
            currentLanguage = languagePreference == "system" ? DetectSystemLanguage() : languagePreference;

            try
            {
                string speedPath = ScrollSpeedSettingsPath();
                int savedIndex;
                if (File.Exists(speedPath) &&
                    Int32.TryParse(File.ReadAllText(speedPath).Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out savedIndex) &&
                    savedIndex >= 0 && savedIndex <= 3)
                    scrollSpeedIndex = savedIndex;
            }
            catch { scrollSpeedIndex = 0; }

            try
            {
                string fontPath = FontSizeSettingsPath();
                int savedIndex;
                if (File.Exists(fontPath) &&
                    Int32.TryParse(File.ReadAllText(fontPath).Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out savedIndex) &&
                    savedIndex >= 0 && savedIndex <= 4)
                    fontSizeIndex = savedIndex;
            }
            catch { fontSizeIndex = 1; }
        }

        private static string SettingsPath()
        {
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SLRR Class Reader", "language.txt");
        }

        private static string ScrollSpeedSettingsPath()
        {
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SLRR Class Reader", "scroll-speed.txt");
        }

        private static string FontSizeSettingsPath()
        {
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "SLRR Class Reader", "font-size.txt");
        }

        private static float GetSourceFontSize(int index)
        {
            switch (index)
            {
                case 0: return 8.0F;
                case 2: return 11.0F;
                case 3: return 12.5F;
                case 4: return 14.0F;
                default: return 9.5F;
            }
        }

        private static double GetScrollSpeedValue(int index)
        {
            switch (index)
            {
                case 1: return 1.5;
                case 2: return 2.0;
                case 3: return 3.0;
                default: return 1.0;
            }
        }

        private static string DetectSystemLanguage()
        {
            return CultureInfo.CurrentUICulture.Name.StartsWith("ru", StringComparison.OrdinalIgnoreCase) ? "ru" : "en";
        }

        private void SaveSettings()
        {
            try
            {
                string path = SettingsPath();
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                File.WriteAllText(path, languagePreference, new System.Text.UTF8Encoding(false));
            }
            catch (Exception error) { SetStatus(R("Не удалось сохранить язык: ", "Could not save language: ") + error.Message); }
        }

        private void OnLanguageChanged(object sender, EventArgs e)
        {
            if (!IsHandleCreated || languageBox.SelectedIndex < 0) return;
            languagePreference = languageBox.SelectedIndex == 1 ? "ru" : languageBox.SelectedIndex == 2 ? "en" : "system";
            currentLanguage = languagePreference == "system" ? DetectSystemLanguage() : languagePreference;
            SaveSettings();
            ApplyLanguage();
            UpdateSelectedResult(false);
            UpdateButtons();
            resultList.Invalidate();
            RefreshBatchStatus();
        }

        private void OnScrollSpeedChanged(object sender, EventArgs e)
        {
            if (!IsHandleCreated || scrollSpeedBox.SelectedIndex < 0) return;
            scrollSpeedIndex = scrollSpeedBox.SelectedIndex;
            sourceBox.ScrollSpeed = GetScrollSpeedValue(scrollSpeedIndex);
            try
            {
                string path = ScrollSpeedSettingsPath();
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                File.WriteAllText(path, scrollSpeedIndex.ToString(CultureInfo.InvariantCulture), new System.Text.UTF8Encoding(false));
            }
            catch (Exception error)
            {
                SetStatus(R("Не удалось сохранить скорость прокрутки: ", "Could not save scroll speed: ") + error.Message);
            }
        }

        private void OnFontSizeChanged(object sender, EventArgs e)
        {
            if (!IsHandleCreated || fontSizeBox.SelectedIndex < 0) return;
            fontSizeIndex = fontSizeBox.SelectedIndex;
            try
            {
                string path = FontSizeSettingsPath();
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                File.WriteAllText(path, fontSizeIndex.ToString(CultureInfo.InvariantCulture), new System.Text.UTF8Encoding(false));
            }
            catch (Exception error)
            {
                SetStatus(R("Не удалось сохранить размер шрифта: ", "Could not save font size: ") + error.Message);
            }
            ApplySourceFontSize();
        }

        private void ApplySourceFontSize()
        {
            float fontSize = GetSourceFontSize(fontSizeIndex);
            ResultEntry entry = resultList.SelectedItem as ResultEntry;
            bool refreshSource = entry != null && entry.Result.Success && sourceBox.IsHandleCreated;
            int firstVisibleLine = refreshSource ? sourceBox.FirstVisibleLine : 0;
            int selectionStart = refreshSource ? sourceBox.SelectionStart : 0;
            int selectionLength = refreshSource ? sourceBox.SelectionLength : 0;

            // Keep every Font alive while any control might still reference it.
            // In particular, WinForms can treat two equal Font values as the
            // same assignment and retain the original object.
            sourceBox.Font = sourceViewerFonts[fontSizeIndex];

            if (refreshSource)
            {
                sourceBox.Rtf = JavaSyntaxHighlighter.Build(entry.Result.Source ?? "", fontSize);
                selectionStart = Math.Min(selectionStart, sourceBox.TextLength);
                selectionLength = Math.Min(selectionLength, sourceBox.TextLength - selectionStart);
                sourceBox.Select(selectionStart, selectionLength);
                int currentFirstVisibleLine = sourceBox.FirstVisibleLine;
                int lineDelta = firstVisibleLine - currentFirstVisibleLine;
                if (lineDelta != 0)
                    SendMessage(sourceBox.Handle, 0x00B6, IntPtr.Zero, new IntPtr(lineDelta));
            }
            lineNumberGutter.Invalidate();
        }

        private void RefreshBatchStatus()
        {
            if (busy)
            {
                SetStatus(R("Обработка файлов…", "Processing files…"));
            }
            else if (hasBatchSummary)
            {
                SetStatus(FormatBatchSummary());
            }
            else if (resultList.Items.Count == 0)
            {
                SetStatus(R("Готов к работе", "Ready"));
            }
        }

        private string FormatBatchSummary()
        {
            string unit = R(" мс", " ms");
            string engineTime = lastBatchEngineMs.ToString("0.00", CultureInfo.InvariantCulture) + unit;
            string wallTime = lastBatchWallMs.ToString("0.00", CultureInfo.InvariantCulture) + unit;
            if (lastBatchFailed > 0)
                return R("Готово: ", "Done: ") + lastBatchSucceeded +
                    R(" успешно, ошибок: ", " succeeded, errors: ") + lastBatchFailed +
                    "  ·  " + R("время обработки: ", "engine: ") + engineTime +
                    "  ·  " + R("общее время: ", "wall: ") + wallTime;
            return R("Готово. Файлов: ", "Done. Files: ") + lastBatchSucceeded +
                "  ·  " + R("время обработки: ", "engine: ") + engineTime +
                "  ·  " + R("общее время: ", "wall: ") + wallTime;
        }

        private void ApplyLanguage()
        {
            fontSizeLabel.Text = R("ШРИФТ", "FONT");
            fontSizeLabel.AccessibleName = R("Размер шрифта", "Font size");
            openButton.Text = R("Открыть .class", "Open .class");
            revealButton.Text = R("Показать в папке", "Show in folder");
            copyButton.Text = R("Копировать код", "Copy code");
            languageLabel.Text = R("ЯЗЫК", "LANGUAGE");
            scrollSpeedLabel.Text = R("СКОРОСТЬ ПРОКРУТКИ", "SCROLL SPEED");
            welcomeLabel.Text = "SLRR Class Reader";
            welcomeSymbolLabel.Text = ".class     →     .java";
            welcomeDescriptionLabel.Text = R("Читаемый Java-подобный исходный код из игровых .class-файлов.", "Readable Java-like source reconstructed from game .class files.");
            dropTitleLabel.Text = R("ПЕРЕТАЩИТЕ СЮДА ФАЙЛЫ .CLASS", "DROP .CLASS FILES HERE");
            dropSubtitleLabel.Text = R("или выберите файлы вручную", "or choose files manually");
            welcomeOpenButton.Text = R("Выбрать файлы .class", "Choose .class files");
            welcomeFootnoteLabel.Text = R("ИСХОДНЫЕ CLASS НЕ ИЗМЕНЯЮТСЯ  ·  JAVA СОХРАНЯЕТСЯ РЯДОМ", "INPUT CLASS FILES ARE NEVER MODIFIED  ·  JAVA IS SAVED BESIDE THE CLASS");
            resultsHeaderLabel.Text = R("РЕЗУЛЬТАТЫ", "RESULTS");
            emptyResultsLabel.Text = R("Пока нет результатов\r\nОткройте .class-файл", "No files yet\r\nOpen a .class file");
            diagnosticsHeader.Text = R("ДИАГНОСТИКА", "DIAGNOSTICS");
            outputLabel.Text = R("СОХРАНЕНО В", "SAVED TO");
            resultList.Invalidate();
            if (resultList.Items.Count == 0) SetStatus(R("Готов к работе", "Ready"));
        }

        private async Task OpenDialog()
        {
            using (OpenFileDialog dialog = new OpenFileDialog())
            {
                dialog.Title = R("Открыть файлы SLRR .class", "Open SLRR .class files");
                dialog.Filter = R("Файлы SLRR .class|*.class|Все файлы|*.*", "SLRR .class files|*.class|All files|*.*");
                dialog.Multiselect = true;
                if (dialog.ShowDialog(this) == DialogResult.OK) await ProcessPaths(dialog.FileNames);
            }
        }

        private async Task ProcessPaths(IEnumerable<string> paths)
        {
            string[] normalized = (paths ?? new string[0]).Where(p => !String.IsNullOrWhiteSpace(p)).Select(Path.GetFullPath)
                .Where(p => p.EndsWith(".class", StringComparison.OrdinalIgnoreCase)).Distinct(StringComparer.OrdinalIgnoreCase).ToArray();
            if (normalized.Length == 0) return;
            if (busy) { SetStatus(R("Дождись завершения текущей обработки.", "Wait for the current operation to finish.")); return; }

            Stopwatch batchTimer = Stopwatch.StartNew();
            busy = true;
            UpdateButtons();
            SetStatus(R("Обработка файлов…", "Processing files…"));
            List<FileDecompileResult> processed;
            try
            {
                processed = await Task.Run(delegate
                {
                    List<FileDecompileResult> batch = new List<FileDecompileResult>();
                    foreach (string file in normalized) batch.Add(DecompilerService.ProcessFile(file));
                    return batch;
                });
            }
            catch (Exception error)
            {
                busy = false; UpdateButtons(); SetStatus(R("Ошибка обработки: ", "Processing failed: ") + error.Message); return;
            }

            foreach (FileDecompileResult result in processed)
            {
                ResultEntry entry = new ResultEntry { Result = result, DisplayName = GetDisplayName(result) };
                entries.Insert(0, entry);
            }
            RefreshResults();
            busy = false;
            UpdateButtons();
            if (resultList.Items.Count > 0) resultList.SelectedIndex = 0;
            batchTimer.Stop();
            int failed = processed.Count(item => !item.Success);
            int succeeded = processed.Count - failed;
            lastBatchSucceeded = succeeded;
            lastBatchFailed = failed;
            lastBatchEngineMs = processed.Sum(item => item.Timings.TotalMs);
            lastBatchWallMs = batchTimer.Elapsed.TotalMilliseconds;
            hasBatchSummary = true;
            SetStatus(FormatBatchSummary());
        }

        private string GetDisplayName(FileDecompileResult result)
        {
            string name = Path.GetFileName(result.InputPath);
            if (!result.Success) return "✕  " + name;
            return (result.Diagnostics.Count > 0 ? "⚠  " : "✓  ") + name;
        }

        private void RefreshResults()
        {
            int oldIndex = resultList.SelectedIndex;
            resultList.BeginUpdate();
            resultList.Items.Clear();
            foreach (ResultEntry entry in entries) resultList.Items.Add(entry);
            resultList.EndUpdate();
            resultCountLabel.Text = entries.Count.ToString(CultureInfo.InvariantCulture);
            emptyResultsLabel.Visible = entries.Count == 0;
            hoveredResultIndex = -1;
            if (oldIndex >= 0 && oldIndex < resultList.Items.Count) resultList.SelectedIndex = oldIndex;
            resultList.Invalidate();
        }

        private void DrawResultItem(object sender, DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= resultList.Items.Count) return;
            ResultEntry entry = resultList.Items[e.Index] as ResultEntry;
            if (entry == null) return;

            bool selected = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            bool hovered = e.Index == hoveredResultIndex;
            e.Graphics.SmoothingMode = SmoothingMode.AntiAlias;
            using (Brush backgroundBrush = new SolidBrush(panel)) e.Graphics.FillRectangle(backgroundBrush, e.Bounds);

            Rectangle card = new Rectangle(e.Bounds.Left + 4, e.Bounds.Top + 3, Math.Max(20, e.Bounds.Width - 8), Math.Max(20, e.Bounds.Height - 6));
            Color cardColor = selected ? Color.FromArgb(40, 35, 28) : hovered ? panelHover : panel;
            using (GraphicsPath path = RoundedRectangle(card, 7))
            using (Brush brush = new SolidBrush(cardColor)) e.Graphics.FillPath(brush, path);
            if (selected)
            {
                using (GraphicsPath path = RoundedRectangle(card, 7))
                using (Pen pen = new Pen(Color.FromArgb(91, 73, 48))) e.Graphics.DrawPath(pen, path);
            }

            Color statusColor = !entry.Result.Success ? danger : entry.Result.Diagnostics.Count > 0 ? warning : success;
            Rectangle iconBox = new Rectangle(card.Left + 10, card.Top + 11, 27, 34);
            using (GraphicsPath path = RoundedRectangle(iconBox, 5))
            using (Brush brush = new SolidBrush(Color.FromArgb(40, 35, 28))) e.Graphics.FillPath(brush, path);
            using (GraphicsPath path = RoundedRectangle(iconBox, 5))
            using (Pen pen = new Pen(Color.FromArgb(91, 73, 48))) e.Graphics.DrawPath(pen, path);

            string glyph = !entry.Result.Success ? "×" : entry.Result.Diagnostics.Count > 0 ? "!" : "J";
            using (Font iconFont = new Font("Segoe UI", 11F, FontStyle.Bold))
                TextRenderer.DrawText(e.Graphics, glyph, iconFont, iconBox, statusColor, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter | TextFormatFlags.NoPadding);

            Rectangle textArea = new Rectangle(iconBox.Right + 10, card.Top + 7, Math.Max(24, card.Right - iconBox.Right - 20), 22);
            string fileName = Path.GetFileName(entry.Result.InputPath);
            using (Font titleFont = new Font("Segoe UI", 8.5F, FontStyle.Bold))
                TextRenderer.DrawText(e.Graphics, fileName, titleFont, textArea, foreground, TextFormatFlags.Left | TextFormatFlags.VerticalCenter | TextFormatFlags.SingleLine | TextFormatFlags.EndEllipsis);

            string detail = entry.Result.Success
                ? Path.GetFileName(entry.Result.OutputPath) + "  ·  " + entry.Result.Timings.TotalMs.ToString("0.00", CultureInfo.InvariantCulture) + R(" мс", " ms")
                : (entry.Result.Error ?? R("Ошибка обработки", "Processing failed"));
            Rectangle detailArea = new Rectangle(textArea.Left, card.Top + 29, textArea.Width, 19);
            using (Font detailFont = new Font("Segoe UI", 7.5F))
                TextRenderer.DrawText(e.Graphics, detail, detailFont, detailArea, entry.Result.Success ? dim : danger, TextFormatFlags.Left | TextFormatFlags.VerticalCenter | TextFormatFlags.SingleLine | TextFormatFlags.EndEllipsis);
        }

        private void OnResultListMouseMove(object sender, MouseEventArgs e)
        {
            int index = resultList.IndexFromPoint(e.Location);
            if (index == hoveredResultIndex) return;
            int previous = hoveredResultIndex;
            hoveredResultIndex = index;
            if (previous >= 0 && previous < resultList.Items.Count) resultList.Invalidate(resultList.GetItemRectangle(previous));
            if (index >= 0 && index < resultList.Items.Count) resultList.Invalidate(resultList.GetItemRectangle(index));
        }

        private void OnResultListMouseLeave(object sender, EventArgs e)
        {
            int previous = hoveredResultIndex;
            hoveredResultIndex = -1;
            if (previous >= 0 && previous < resultList.Items.Count) resultList.Invalidate(resultList.GetItemRectangle(previous));
        }

        private void OnSelectionChanged(object sender, EventArgs e)
        {
            UpdateSelectedResult(true);
        }

        private void UpdateSelectedResult(bool reloadSource)
        {
            ResultEntry entry = resultList.SelectedItem as ResultEntry;
            if (entry == null) { readerPanel.Visible = false; welcomePanel.Visible = true; UpdateButtons(); return; }
            welcomePanel.Visible = false; readerPanel.Visible = true;
            if (!entry.Result.Success)
            {
                fileBadgeLabel.Text = "ERR";
                fileBadgeLabel.ForeColor = danger;
                fileBadgeLabel.BackColor = Color.FromArgb(43, 29, 31);
                titleLabel.Text = Path.GetFileName(entry.Result.InputPath);
                fileLabel.Text = R("Ошибка декомпиляции", "Decompilation failed");
                if (reloadSource)
                    sourceBox.Text = entry.Result.Error ?? R("Неизвестная ошибка", "Unknown error");
                diagnosticsBox.Visible = false;
                diagnosticsPanel.Visible = false;
                readerLayout.RowStyles[1].Height = 0F;
                lineLabel.Text = "—";
                outputLabel.Text = R("ВЫХОДНОЙ ФАЙЛ НЕ СОЗДАН", "NO OUTPUT FILE CREATED");
                performanceLabel.Text = FormatTimings(entry.Result.Timings);
            }
            else
            {
                fileBadgeLabel.Text = "JAVA";
                fileBadgeLabel.ForeColor = accent;
                fileBadgeLabel.BackColor = Color.FromArgb(40, 35, 28);
                titleLabel.Text = GetClassTitle(entry.Result);
                fileLabel.Text = Path.GetFileName(entry.Result.OutputPath);
                if (reloadSource)
                    sourceBox.Rtf = JavaSyntaxHighlighter.Build(entry.Result.Source ?? "", GetSourceFontSize(fontSizeIndex));
                lineLabel.Text = (entry.Result.Source ?? "").Count(c => c == '\n') + R(" строк", " lines");
                diagnosticsBox.Visible = entry.Result.Diagnostics.Count > 0;
                diagnosticsPanel.Visible = diagnosticsBox.Visible;
                readerLayout.RowStyles[1].Height = diagnosticsPanel.Visible ? 90F : 0F;
                diagnosticsBox.Text = String.Join(Environment.NewLine, entry.Result.Diagnostics.ToArray());
                outputLabel.Text = R("СОХРАНЕНО В: ", "SAVED TO: ") + entry.Result.OutputPath;
                performanceLabel.Text = FormatTimings(entry.Result.Timings);
            }
            if (reloadSource)
            {
                sourceBox.SelectionStart = 0;
                sourceBox.SelectionLength = 0;
                sourceBox.ScrollToCaret();
                ResetSourceHorizontalScroll();
            }
            lineNumberGutter.Invalidate();
            LayoutReaderPane();
            UpdateButtons();
        }

        private string FormatTimings(DecompileTimings timing)
        {
            if (timing == null) return "";
            string format = "0.00";
            return R("Время (мс): чтение ", "Timing (ms): read ") + timing.ReadMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("разбор TUFA ", "TUFA parse ") + timing.ParseMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("декомпиляция ", "decompile ") + timing.DecompileMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("форматирование ", "format ") + timing.FormatMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("сборка ", "combine ") + timing.CombineMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("запись ", "write ") + timing.WriteMs.ToString(format, CultureInfo.InvariantCulture) + "  ·  " +
                R("всего ", "total ") + timing.TotalMs.ToString(format, CultureInfo.InvariantCulture);
        }

        private string GetClassTitle(FileDecompileResult result)
        {
            if (result.ClassCount <= 1) return result.ClassName;
            int n = result.ClassCount;
            string countText;
            if (currentLanguage == "ru")
            {
                int lastTwo = n % 100;
                int last = n % 10;
                string word = lastTwo >= 11 && lastTwo <= 14 ? "классов" : last == 1 ? "класс" : last >= 2 && last <= 4 ? "класса" : "классов";
                countText = n.ToString(CultureInfo.InvariantCulture) + " " + word;
            }
            else countText = n.ToString(CultureInfo.InvariantCulture) + (n == 1 ? " class" : " classes");
            return result.ClassName + " — " + countText;
        }

        private void LayoutReaderPane()
        {
            if (readerHeaderPanel == null || diagnosticsPanel == null || outputPanel == null) return;
            int headerWidth = Math.Max(100, readerHeaderPanel.ClientSize.Width);
            lineLabel.Left = Math.Max(fileBadgeLabel.Right + 12, headerWidth - lineLabel.Width);
            titleLabel.Width = Math.Max(100, headerWidth - titleLabel.Left - lineLabel.Width - 16);
            fileLabel.Width = Math.Max(100, headerWidth - fileLabel.Left);
            performanceLabel.Width = Math.Max(100, headerWidth - performanceLabel.Left);
            diagnosticsBox.Width = Math.Max(100, diagnosticsPanel.ClientSize.Width);
            fontSizeBox.Left = Math.Max(0, outputPanel.ClientSize.Width - fontSizeBox.Width - 10);
            fontSizeBox.Top = 7;
            fontSizeBox.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            fontSizeLabel.Left = fontSizeBox.Left - fontSizeLabel.Width - 3;
            fontSizeLabel.Top = 9;
            fontSizeLabel.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            outputLabel.Width = Math.Max(100, fontSizeLabel.Left - 12);
        }

        private void LayoutWelcomePanel()
        {
            if (welcomePanel == null || welcomePanel.ClientSize.Width <= 0 || welcomePanel.ClientSize.Height <= 0) return;
            int width = welcomePanel.ClientSize.Width;
            int height = welcomePanel.ClientSize.Height;
            int startY = Math.Max(26, (height - 360) / 2);
            int centerX = width / 2;
            int contentWidth = Math.Min(560, Math.Max(300, width - 48));
            int left = centerX - contentWidth / 2;

            welcomeSymbolLabel.Bounds = new Rectangle(left, startY, contentWidth, 30);
            welcomeLabel.Bounds = new Rectangle(left, startY + 42, contentWidth, 40);
            welcomeDescriptionLabel.Bounds = new Rectangle(left, startY + 88, contentWidth, 42);

            int dropWidth = Math.Min(470, Math.Max(300, width - 64));
            dropPanel.Bounds = new Rectangle(centerX - dropWidth / 2, startY + 145, dropWidth, 150);
            dropTitleLabel.Bounds = new Rectangle(18, 19, dropWidth - 36, 24);
            dropSubtitleLabel.Bounds = new Rectangle(18, 46, dropWidth - 36, 20);
            welcomeOpenButton.Location = new Point((dropWidth - welcomeOpenButton.Width) / 2, 88);

            welcomeFootnoteLabel.Bounds = new Rectangle(left, startY + 312, contentWidth, 28);
        }

        private void PaintDropPanel(object sender, PaintEventArgs e)
        {
            e.Graphics.SmoothingMode = SmoothingMode.AntiAlias;
            using (Pen pen = new Pen(Color.FromArgb(71, 81, 95), 1F))
            {
                pen.DashStyle = DashStyle.Dash;
                using (GraphicsPath path = RoundedRectangle(new Rectangle(1, 1, dropPanel.Width - 3, dropPanel.Height - 3), 9))
                    e.Graphics.DrawPath(pen, path);
            }
        }

        private static GraphicsPath RoundedRectangle(Rectangle bounds, int radius)
        {
            GraphicsPath path = new GraphicsPath();
            int diameter = Math.Max(2, radius * 2);
            Rectangle arc = new Rectangle(bounds.Left, bounds.Top, diameter, diameter);
            path.AddArc(arc, 180, 90);
            arc.X = bounds.Right - diameter;
            path.AddArc(arc, 270, 90);
            arc.Y = bounds.Bottom - diameter;
            path.AddArc(arc, 0, 90);
            arc.X = bounds.Left;
            path.AddArc(arc, 90, 90);
            path.CloseFigure();
            return path;
        }

        private void UpdateButtons()
        {
            ResultEntry entry = resultList.SelectedItem as ResultEntry;
            openButton.Enabled = !busy;
            bool successful = entry != null && entry.Result.Success;
            copyButton.Enabled = successful && !busy;
            revealButton.Enabled = successful && !busy;
        }

        private void CopySelected(object sender, EventArgs e)
        {
            ResultEntry entry = resultList.SelectedItem as ResultEntry;
            if (entry == null || !entry.Result.Success) return;
            try { Clipboard.SetText(entry.Result.Source ?? ""); SetStatus(R("Код скопирован", "Source copied")); }
            catch (Exception error) { SetStatus(R("Не удалось скопировать: ", "Could not copy: ") + error.Message); }
        }

        private void RevealSelected(object sender, EventArgs e)
        {
            ResultEntry entry = resultList.SelectedItem as ResultEntry;
            if (entry == null || !entry.Result.Success) return;
            try { System.Diagnostics.Process.Start("explorer.exe", "/select,\"" + entry.Result.OutputPath + "\""); }
            catch (Exception error) { SetStatus(R("Не удалось открыть папку: ", "Could not open folder: ") + error.Message); }
        }

        private void SetStatus(string text)
        {
            statusLabel.Text = text;
            bool hasError = text.IndexOf("error", StringComparison.OrdinalIgnoreCase) >= 0 ||
                text.IndexOf("ошиб", StringComparison.OrdinalIgnoreCase) >= 0;
            statusIndicatorLabel.ForeColor = busy ? accent : hasError ? danger : success;
        }

        private void ConfigureDropTarget(Control control)
        {
            control.AllowDrop = true;
            control.DragEnter += OnDragEnter;
            control.DragDrop += OnDragDrop;
        }

        private void OnDragEnter(object sender, DragEventArgs e)
        {
            if (!busy && e.Data.GetDataPresent(DataFormats.FileDrop)) e.Effect = DragDropEffects.Copy;
            else e.Effect = DragDropEffects.None;
        }

        private async void OnDragDrop(object sender, DragEventArgs e)
        {
            string[] paths = e.Data.GetData(DataFormats.FileDrop) as string[];
            if (paths != null) await ProcessPaths(paths);
        }

        private void OnFormClosing(object sender, FormClosingEventArgs e)
        {
            if (busy && e.CloseReason == CloseReason.UserClosing)
            {
                e.Cancel = true;
                SetStatus(R("Дождись завершения обработки перед закрытием.", "Wait for processing to finish before closing."));
            }
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                // Dispose controls first; they may still use one of the cached fonts.
                base.Dispose(disposing);
                for (int i = 0; i < sourceViewerFonts.Length; i++)
                    sourceViewerFonts[i].Dispose();
                return;
            }
            base.Dispose(disposing);
        }

        private sealed class SmoothScrollingRichTextBox : RichTextBox
        {
            private const int WM_VSCROLL = 0x0115;
            private const int WM_HSCROLL = 0x0114;
            private const int WM_KEYUP = 0x0101;
            private const int WM_LBUTTONUP = 0x0202;
            private const int WM_MOUSEWHEEL = 0x020A;
            private const int EM_GETFIRSTVISIBLELINE = 0x00CE;
            private const int EM_LINESCROLL = 0x00B6;

            [DllImport("user32.dll", CharSet = CharSet.Auto)]
            private static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);

            private readonly Timer smoothScrollTimer;
            private int targetFirstVisibleLine;
            private double pendingWheelLines;
            private double scrollStepAccumulator;
            private double scrollSpeed = 1.0;

            public double ScrollSpeed
            {
                get { return scrollSpeed; }
                set
                {
                    scrollSpeed = Math.Max(1.0, Math.Min(3.0, value));
                    scrollStepAccumulator = 0;
                }
            }

            public event EventHandler ViewportChanged;

            public SmoothScrollingRichTextBox()
            {
                smoothScrollTimer = new Timer();
                smoothScrollTimer.Interval = 15;
                smoothScrollTimer.Tick += OnSmoothScrollTick;
            }

            public int FirstVisibleLine
            {
                get
                {
                    if (!IsHandleCreated) return 0;
                    return SendMessage(Handle, EM_GETFIRSTVISIBLELINE, IntPtr.Zero, IntPtr.Zero).ToInt32();
                }
            }

            protected override void OnTextChanged(EventArgs e)
            {
                base.OnTextChanged(e);
                RaiseViewportChanged();
            }

            protected override void OnFontChanged(EventArgs e)
            {
                base.OnFontChanged(e);
                RaiseViewportChanged();
            }

            protected override void OnResize(EventArgs e)
            {
                base.OnResize(e);
                RaiseViewportChanged();
            }

            protected override void WndProc(ref Message m)
            {
                if (m.Msg == WM_MOUSEWHEEL)
                {
                    HandleSmoothWheel(m.WParam);
                    m.Result = IntPtr.Zero;
                    return;
                }

                if (m.Msg == WM_VSCROLL || m.Msg == WM_KEYUP || m.Msg == WM_LBUTTONUP)
                {
                    smoothScrollTimer.Stop();
                    pendingWheelLines = 0;
                    scrollStepAccumulator = 0;
                }

                base.WndProc(ref m);
                if (m.Msg == WM_VSCROLL || m.Msg == WM_HSCROLL ||
                    m.Msg == WM_KEYUP || m.Msg == WM_LBUTTONUP)
                {
                    if (m.Msg == WM_VSCROLL || m.Msg == WM_KEYUP)
                        targetFirstVisibleLine = FirstVisibleLine;
                    RaiseViewportChanged();
                }
            }

            private void HandleSmoothWheel(IntPtr wParam)
            {
                int delta = unchecked((short)(wParam.ToInt64() >> 16));
                if (delta == 0 || !IsHandleCreated) return;

                int configuredLines = SystemInformation.MouseWheelScrollLines;
                if (configuredLines == 0) return;

                int currentLine = FirstVisibleLine;
                if (!smoothScrollTimer.Enabled) targetFirstVisibleLine = currentLine;

                double linesPerNotch = configuredLines < 0
                    ? Math.Max(1, ClientSize.Height / Math.Max(1, Font.Height))
                    : configuredLines;

                pendingWheelLines -= (delta / 120.0) * linesPerNotch;
                int wholeLines = (int)pendingWheelLines;
                if (wholeLines != 0)
                {
                    pendingWheelLines -= wholeLines;
                    targetFirstVisibleLine = Math.Max(0, targetFirstVisibleLine + wholeLines);
                }

                if (targetFirstVisibleLine == currentLine)
                {
                    smoothScrollTimer.Stop();
                    scrollStepAccumulator = 0;
                    return;
                }

                if (!smoothScrollTimer.Enabled) smoothScrollTimer.Start();
            }

            private void OnSmoothScrollTick(object sender, EventArgs e)
            {
                if (!IsHandleCreated)
                {
                    smoothScrollTimer.Stop();
                    return;
                }

                int currentLine = FirstVisibleLine;
                int distance = targetFirstVisibleLine - currentLine;
                if (distance == 0)
                {
                    smoothScrollTimer.Stop();
                    scrollStepAccumulator = 0;
                    RaiseViewportChanged();
                    return;
                }

                scrollStepAccumulator += scrollSpeed;
                int stepMagnitude = Math.Max(1, (int)scrollStepAccumulator);
                scrollStepAccumulator -= stepMagnitude;
                stepMagnitude = Math.Min(Math.Abs(distance), stepMagnitude);
                int step = distance > 0 ? stepMagnitude : -stepMagnitude;
                SendMessage(Handle, EM_LINESCROLL, IntPtr.Zero, new IntPtr(step));

                int actualLine = FirstVisibleLine;
                if (actualLine == currentLine && actualLine != targetFirstVisibleLine)
                {
                    targetFirstVisibleLine = actualLine;
                    pendingWheelLines = 0;
                    scrollStepAccumulator = 0;
                    smoothScrollTimer.Stop();
                }
                else if (actualLine == targetFirstVisibleLine)
                {
                    scrollStepAccumulator = 0;
                    smoothScrollTimer.Stop();
                }

                RaiseViewportChanged();
            }

            private void RaiseViewportChanged()
            {
                EventHandler handler = ViewportChanged;
                if (handler != null) handler(this, EventArgs.Empty);
            }

            protected override void Dispose(bool disposing)
            {
                if (disposing)
                {
                    smoothScrollTimer.Stop();
                    smoothScrollTimer.Dispose();
                }
                base.Dispose(disposing);
            }
        }

        private sealed class LineNumberGutter : Control
        {
            private readonly SmoothScrollingRichTextBox editor;
            private readonly Color normalNumber = Color.FromArgb(88, 99, 115);
            private readonly Color activeNumber = Color.FromArgb(213, 164, 91);
            private readonly Color separator = Color.FromArgb(32, 38, 48);

            public LineNumberGutter(SmoothScrollingRichTextBox editor)
            {
                this.editor = editor;
                SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint |
                    ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
                TabStop = false;
            }

            protected override void OnPaint(PaintEventArgs e)
            {
                base.OnPaint(e);
                e.Graphics.Clear(BackColor);
                using (Pen pen = new Pen(separator))
                    e.Graphics.DrawLine(pen, Width - 1, 0, Width - 1, Height);

                if (editor == null || !editor.IsHandleCreated) return;

                int totalLines = Math.Max(1, editor.GetLineFromCharIndex(editor.TextLength) + 1);
                int firstLine = Math.Max(0, editor.FirstVisibleLine);
                int activeLine = editor.GetLineFromCharIndex(editor.SelectionStart);
                using (Font numberFont = new Font("Consolas", Math.Max(7.0F, editor.Font.Size - 1.0F), FontStyle.Regular, GraphicsUnit.Point))
                {
                    for (int line = firstLine; line < totalLines; line++)
                    {
                        int charIndex = editor.GetFirstCharIndexFromLine(line);
                        if (charIndex < 0)
                        {
                            if (line != totalLines - 1) break;
                            charIndex = editor.TextLength;
                        }

                        Point position = editor.GetPositionFromCharIndex(charIndex);
                        int lineHeight = Math.Max(editor.Font.Height, numberFont.Height);
                        if (position.Y > Height) break;
                        if (position.Y + lineHeight < 0) continue;

                        Rectangle bounds = new Rectangle(0, position.Y, Width - 9, lineHeight);
                        Color color = line == activeLine ? activeNumber : normalNumber;
                        TextRenderer.DrawText(e.Graphics, (line + 1).ToString(CultureInfo.InvariantCulture),
                            numberFont, bounds, color,
                            TextFormatFlags.Right | TextFormatFlags.VerticalCenter |
                            TextFormatFlags.SingleLine | TextFormatFlags.NoPadding);
                    }
                }
            }
        }

        private static class JavaSyntaxHighlighter
        {
            private const int DefaultColor = 1;
            private const int KeywordColor = 2;
            private const int StringColor = 3;
            private const int CommentColor = 4;
            private const int NumberColor = 5;
            private const int LiteralColor = 6;
            private const int AnnotationColor = 7;
            private const int TypeColor = 8;

            private static readonly HashSet<string> Keywords = new HashSet<string>(new string[]
            {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while", "var",
                "record", "sealed", "permits", "yield", "non-sealed"
            }, StringComparer.Ordinal);

            private static readonly HashSet<string> Literals = new HashSet<string>(new string[]
            {
                "true", "false", "null"
            }, StringComparer.Ordinal);

            private static readonly HashSet<string> TypeNames = new HashSet<string>(new string[]
            {
                "String", "Object", "Class", "System", "Math", "Thread",
                "Throwable", "Exception", "RuntimeException", "Error",
                "Integer", "Long", "Short", "Byte", "Boolean", "Character",
                "Double", "Float", "Void", "StringBuilder", "StringBuffer"
            }, StringComparer.Ordinal);

            public static string Build(string source, float fontSize)
            {
                if (source == null) source = String.Empty;

                StringBuilder output = new StringBuilder((int)Math.Min(10000000L, (long)source.Length * 2 + 256));
                output.Append(@"{\rtf1\ansi\deff0\uc1");
                output.Append(@"{\fonttbl{\f0\fnil\fcharset0 Consolas;}}");
                output.Append(@"{\colortbl;");
                AppendColor(output, 213, 219, 229);
                AppendColor(output, 197, 160, 240);
                AppendColor(output, 155, 190, 134);
                AppendColor(output, 111, 124, 119);
                AppendColor(output, 219, 177, 117);
                AppendColor(output, 220, 151, 105);
                AppendColor(output, 120, 175, 190);
                AppendColor(output, 119, 170, 216);
                output.Append(@"}\viewkind4\pard\sa0\sb0\f0\fs");
                output.Append(Math.Max(1, (int)Math.Round(fontSize * 2.0F)).ToString(CultureInfo.InvariantCulture));
                output.Append(@"\cf1 ");

                int currentColor = DefaultColor;
                bool expectTypeName = false;
                int i = 0;
                while (i < source.Length)
                {
                    int start = i;
                    int color = DefaultColor;
                    char c = source[i];
                    char next = i + 1 < source.Length ? source[i + 1] : '\0';

                    if (c == '/' && next == '/')
                    {
                        color = CommentColor;
                        i += 2;
                        while (i < source.Length && source[i] != '\r' && source[i] != '\n') i++;
                    }
                    else if (c == '/' && next == '*')
                    {
                        color = CommentColor;
                        i += 2;
                        while (i < source.Length - 1 && !(source[i] == '*' && source[i + 1] == '/')) i++;
                        if (i < source.Length - 1) i += 2;
                        else i = source.Length;
                    }
                    else if (c == '"' || c == '\'')
                    {
                        color = StringColor;
                        char quote = c;
                        i = ScanQuoted(source, i, quote);
                    }
                    else if (c == '@' && i + 1 < source.Length && IsIdentifierStart(source[i + 1]))
                    {
                        color = AnnotationColor;
                        i += 2;
                        while (i < source.Length && IsIdentifierPart(source[i])) i++;
                    }
                    else if (Char.IsDigit(c))
                    {
                        color = NumberColor;
                        i++;
                        while (i < source.Length)
                        {
                            char digitChar = source[i];
                            if (Char.IsLetterOrDigit(digitChar) || digitChar == '_' || digitChar == '.') i++;
                            else if ((digitChar == '+' || digitChar == '-') && i > start &&
                                (source[i - 1] == 'e' || source[i - 1] == 'E' || source[i - 1] == 'p' || source[i - 1] == 'P')) i++;
                            else break;
                        }
                    }
                    else if (IsIdentifierStart(c))
                    {
                        i++;
                        while (i < source.Length && IsIdentifierPart(source[i])) i++;
                        string word = source.Substring(start, i - start);
                        if (expectTypeName)
                        {
                            color = TypeColor;
                            expectTypeName = false;
                        }
                        else if (Keywords.Contains(word))
                        {
                            color = KeywordColor;
                            expectTypeName = word == "class" || word == "interface" ||
                                word == "enum" || word == "record" || word == "new" ||
                                word == "extends" || word == "implements" ||
                                word == "instanceof" || word == "throws";
                        }
                        else if (Literals.Contains(word)) color = LiteralColor;
                        else if (TypeNames.Contains(word)) color = TypeColor;
                    }
                    else
                    {
                        if (!Char.IsWhiteSpace(c)) expectTypeName = false;
                        i++;
                    }

                    if (color != currentColor)
                    {
                        output.Append(@"\cf").Append(color.ToString(CultureInfo.InvariantCulture)).Append(' ');
                        currentColor = color;
                    }
                    AppendEscapedText(output, source, start, i - start);
                }

                output.Append('}');
                return output.ToString();
            }

            private static int ScanQuoted(string source, int start, char quote)
            {
                int i = start + 1;
                while (i < source.Length)
                {
                    if (source[i] == '\\')
                    {
                        i += Math.Min(2, source.Length - i);
                        continue;
                    }
                    if (source[i] == quote)
                    {
                        i++;
                        break;
                    }
                    if (source[i] == '\r' || source[i] == '\n') break;
                    i++;
                }
                return i;
            }

            private static bool IsIdentifierStart(char c)
            {
                return Char.IsLetter(c) || c == '_' || c == '$';
            }

            private static bool IsIdentifierPart(char c)
            {
                return Char.IsLetterOrDigit(c) || c == '_' || c == '$';
            }

            private static void AppendColor(StringBuilder output, int red, int green, int blue)
            {
                output.Append(@"\red").Append(red)
                    .Append(@"\green").Append(green)
                    .Append(@"\blue").Append(blue).Append(';');
            }

            private static void AppendEscapedText(StringBuilder output, string source, int start, int length)
            {
                int end = start + length;
                for (int i = start; i < end; i++)
                {
                    char c = source[i];
                    if (c == '\r')
                    {
                        if (i + 1 < end && source[i + 1] == '\n') i++;
                        output.Append(@"\par").Append('\n');
                    }
                    else if (c == '\n')
                    {
                        output.Append(@"\par").Append('\n');
                    }
                    else if (c == '\t')
                    {
                        output.Append(@"\tab ");
                    }
                    else if (c == '\\')
                    {
                        output.Append(@"\\");
                    }
                    else if (c == '{')
                    {
                        output.Append(@"\{");
                    }
                    else if (c == '}')
                    {
                        output.Append(@"\}");
                    }
                    else if (c >= 32 && c <= 126)
                    {
                        output.Append(c);
                    }
                    else
                    {
                        output.Append(@"\u").Append(unchecked((short)c)).Append('?');
                    }
                }
            }
        }
    }
}
