namespace ResturantManagement.View
{
    partial class frmUserStaffInfo
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle1 = new System.Windows.Forms.DataGridViewCellStyle();
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle2 = new System.Windows.Forms.DataGridViewCellStyle();
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle3 = new System.Windows.Forms.DataGridViewCellStyle();
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(frmUserStaffInfo));
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle4 = new System.Windows.Forms.DataGridViewCellStyle();
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle5 = new System.Windows.Forms.DataGridViewCellStyle();
            System.Windows.Forms.DataGridViewCellStyle dataGridViewCellStyle6 = new System.Windows.Forms.DataGridViewCellStyle();
            this.staff = new System.Windows.Forms.TabPage();
            this.guna2DataGridView1 = new Guna.UI2.WinForms.Guna2DataGridView();
            this.dgvSno = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvid = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvName = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvPhone = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvRole = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvedit = new System.Windows.Forms.DataGridViewImageColumn();
            this.dgvdel = new System.Windows.Forms.DataGridViewImageColumn();
            this.guna2Separator1 = new Guna.UI2.WinForms.Guna2Separator();
            this.panel2 = new System.Windows.Forms.Panel();
            this.label2 = new System.Windows.Forms.Label();
            this.btnAdd = new Guna.UI2.WinForms.Guna2Button();
            this.txtSearch = new Guna.UI2.WinForms.Guna2TextBox();
            this.label1 = new System.Windows.Forms.Label();
            this.panel1 = new System.Windows.Forms.Panel();
            this.lblMgnStaff = new System.Windows.Forms.Label();
            this.metroTabControl1 = new MetroFramework.Controls.MetroTabControl();
            this.user = new System.Windows.Forms.TabPage();
            this.dgvUserInfo = new Guna.UI2.WinForms.Guna2DataGridView();
            this.dgvsnoUser = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvUserid = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvuserame = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvUserphone = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvfullname = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvUserpass = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.dgvuRole = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.editUser = new System.Windows.Forms.DataGridViewImageColumn();
            this.deluser = new System.Windows.Forms.DataGridViewImageColumn();
            this.guna2Separator2 = new Guna.UI2.WinForms.Guna2Separator();
            this.panel3 = new System.Windows.Forms.Panel();
            this.txtSearchUser = new Guna.UI2.WinForms.Guna2TextBox();
            this.label5 = new System.Windows.Forms.Label();
            this.btnAddUser = new Guna.UI2.WinForms.Guna2Button();
            this.label4 = new System.Windows.Forms.Label();
            this.guna2Panel1 = new Guna.UI2.WinForms.Guna2Panel();
            this.label3 = new System.Windows.Forms.Label();
            this.guna2PictureBox1 = new Guna.UI2.WinForms.Guna2PictureBox();
            this.guna2MessageDialog1 = new Guna.UI2.WinForms.Guna2MessageDialog();
            this.guna2MessageDialog2 = new Guna.UI2.WinForms.Guna2MessageDialog();
            this.dataGridViewImageColumn1 = new System.Windows.Forms.DataGridViewImageColumn();
            this.dataGridViewImageColumn2 = new System.Windows.Forms.DataGridViewImageColumn();
            this.lblStaff = new System.Windows.Forms.Label();
            this.lblTotalStaff = new System.Windows.Forms.Label();
            this.totalUser = new System.Windows.Forms.Label();
            this.lblUser = new System.Windows.Forms.Label();
            this.lblMgnUser = new System.Windows.Forms.Label();
            this.staff.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)(this.guna2DataGridView1)).BeginInit();
            this.panel2.SuspendLayout();
            this.panel1.SuspendLayout();
            this.metroTabControl1.SuspendLayout();
            this.user.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)(this.dgvUserInfo)).BeginInit();
            this.panel3.SuspendLayout();
            this.guna2Panel1.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)(this.guna2PictureBox1)).BeginInit();
            this.SuspendLayout();
            // 
            // staff
            // 
            this.staff.BackColor = System.Drawing.Color.White;
            this.staff.Controls.Add(this.guna2DataGridView1);
            this.staff.Controls.Add(this.guna2Separator1);
            this.staff.Controls.Add(this.panel2);
            this.staff.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.staff.Location = new System.Drawing.Point(4, 38);
            this.staff.Name = "staff";
            this.staff.Size = new System.Drawing.Size(1240, 566);
            this.staff.TabIndex = 0;
            this.staff.Text = "Staff";
            // 
            // guna2DataGridView1
            // 
            this.guna2DataGridView1.AllowUserToAddRows = false;
            this.guna2DataGridView1.AllowUserToDeleteRows = false;
            dataGridViewCellStyle1.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(238)))), ((int)(((byte)(239)))), ((int)(((byte)(249)))));
            this.guna2DataGridView1.AlternatingRowsDefaultCellStyle = dataGridViewCellStyle1;
            this.guna2DataGridView1.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            dataGridViewCellStyle2.Alignment = System.Windows.Forms.DataGridViewContentAlignment.MiddleLeft;
            dataGridViewCellStyle2.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(123)))), ((int)(((byte)(143)))), ((int)(((byte)(161)))));
            dataGridViewCellStyle2.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            dataGridViewCellStyle2.ForeColor = System.Drawing.Color.White;
            dataGridViewCellStyle2.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(123)))), ((int)(((byte)(143)))), ((int)(((byte)(161)))));
            dataGridViewCellStyle2.SelectionForeColor = System.Drawing.SystemColors.HighlightText;
            dataGridViewCellStyle2.WrapMode = System.Windows.Forms.DataGridViewTriState.True;
            this.guna2DataGridView1.ColumnHeadersDefaultCellStyle = dataGridViewCellStyle2;
            this.guna2DataGridView1.ColumnHeadersHeight = 40;
            this.guna2DataGridView1.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.EnableResizing;
            this.guna2DataGridView1.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.dgvSno,
            this.dgvid,
            this.dgvName,
            this.dgvPhone,
            this.dgvRole,
            this.dgvedit,
            this.dgvdel});
            dataGridViewCellStyle3.Alignment = System.Windows.Forms.DataGridViewContentAlignment.MiddleLeft;
            dataGridViewCellStyle3.BackColor = System.Drawing.Color.White;
            dataGridViewCellStyle3.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            dataGridViewCellStyle3.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            dataGridViewCellStyle3.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            dataGridViewCellStyle3.SelectionForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            dataGridViewCellStyle3.WrapMode = System.Windows.Forms.DataGridViewTriState.False;
            this.guna2DataGridView1.DefaultCellStyle = dataGridViewCellStyle3;
            this.guna2DataGridView1.GridColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.guna2DataGridView1.Location = new System.Drawing.Point(49, 166);
            this.guna2DataGridView1.Name = "guna2DataGridView1";
            this.guna2DataGridView1.ReadOnly = true;
            this.guna2DataGridView1.RowHeadersVisible = false;
            this.guna2DataGridView1.RowHeadersWidth = 51;
            this.guna2DataGridView1.RowTemplate.DividerHeight = 1;
            this.guna2DataGridView1.RowTemplate.Height = 35;
            this.guna2DataGridView1.Size = new System.Drawing.Size(1144, 311);
            this.guna2DataGridView1.TabIndex = 24;
            this.guna2DataGridView1.ThemeStyle.AlternatingRowsStyle.BackColor = System.Drawing.Color.White;
            this.guna2DataGridView1.ThemeStyle.AlternatingRowsStyle.Font = null;
            this.guna2DataGridView1.ThemeStyle.AlternatingRowsStyle.ForeColor = System.Drawing.Color.Empty;
            this.guna2DataGridView1.ThemeStyle.AlternatingRowsStyle.SelectionBackColor = System.Drawing.Color.Empty;
            this.guna2DataGridView1.ThemeStyle.AlternatingRowsStyle.SelectionForeColor = System.Drawing.Color.Empty;
            this.guna2DataGridView1.ThemeStyle.BackColor = System.Drawing.Color.White;
            this.guna2DataGridView1.ThemeStyle.GridColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(100)))), ((int)(((byte)(88)))), ((int)(((byte)(255)))));
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.BorderStyle = System.Windows.Forms.DataGridViewHeaderBorderStyle.None;
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.ForeColor = System.Drawing.Color.White;
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.HeaightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.EnableResizing;
            this.guna2DataGridView1.ThemeStyle.HeaderStyle.Height = 40;
            this.guna2DataGridView1.ThemeStyle.ReadOnly = true;
            this.guna2DataGridView1.ThemeStyle.RowsStyle.BackColor = System.Drawing.Color.White;
            this.guna2DataGridView1.ThemeStyle.RowsStyle.BorderStyle = System.Windows.Forms.DataGridViewCellBorderStyle.SingleHorizontal;
            this.guna2DataGridView1.ThemeStyle.RowsStyle.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.guna2DataGridView1.ThemeStyle.RowsStyle.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            this.guna2DataGridView1.ThemeStyle.RowsStyle.Height = 35;
            this.guna2DataGridView1.ThemeStyle.RowsStyle.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.guna2DataGridView1.ThemeStyle.RowsStyle.SelectionForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            this.guna2DataGridView1.CellContentClick += new System.Windows.Forms.DataGridViewCellEventHandler(this.guna2DataGridView1_CellContentClick);
            // 
            // dgvSno
            // 
            this.dgvSno.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvSno.HeaderText = "No";
            this.dgvSno.MinimumWidth = 6;
            this.dgvSno.Name = "dgvSno";
            this.dgvSno.ReadOnly = true;
            this.dgvSno.Width = 71;
            // 
            // dgvid
            // 
            this.dgvid.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvid.HeaderText = "id";
            this.dgvid.MinimumWidth = 6;
            this.dgvid.Name = "dgvid";
            this.dgvid.ReadOnly = true;
            this.dgvid.Visible = false;
            this.dgvid.Width = 61;
            // 
            // dgvName
            // 
            this.dgvName.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill;
            this.dgvName.HeaderText = "Name";
            this.dgvName.MinimumWidth = 6;
            this.dgvName.Name = "dgvName";
            this.dgvName.ReadOnly = true;
            // 
            // dgvPhone
            // 
            this.dgvPhone.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill;
            this.dgvPhone.HeaderText = "Phone";
            this.dgvPhone.MinimumWidth = 6;
            this.dgvPhone.Name = "dgvPhone";
            this.dgvPhone.ReadOnly = true;
            // 
            // dgvRole
            // 
            this.dgvRole.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill;
            this.dgvRole.HeaderText = "Role";
            this.dgvRole.MinimumWidth = 6;
            this.dgvRole.Name = "dgvRole";
            this.dgvRole.ReadOnly = true;
            // 
            // dgvedit
            // 
            this.dgvedit.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvedit.HeaderText = "";
            this.dgvedit.Image = ((System.Drawing.Image)(resources.GetObject("dgvedit.Image")));
            this.dgvedit.MinimumWidth = 6;
            this.dgvedit.Name = "dgvedit";
            this.dgvedit.ReadOnly = true;
            this.dgvedit.Width = 6;
            // 
            // dgvdel
            // 
            this.dgvdel.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvdel.HeaderText = "";
            this.dgvdel.Image = ((System.Drawing.Image)(resources.GetObject("dgvdel.Image")));
            this.dgvdel.MinimumWidth = 6;
            this.dgvdel.Name = "dgvdel";
            this.dgvdel.ReadOnly = true;
            this.dgvdel.Width = 6;
            // 
            // guna2Separator1
            // 
            this.guna2Separator1.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.guna2Separator1.Location = new System.Drawing.Point(24, 143);
            this.guna2Separator1.Name = "guna2Separator1";
            this.guna2Separator1.Size = new System.Drawing.Size(1195, 10);
            this.guna2Separator1.TabIndex = 23;
            // 
            // panel2
            // 
            this.panel2.Controls.Add(this.label2);
            this.panel2.Controls.Add(this.btnAdd);
            this.panel2.Controls.Add(this.txtSearch);
            this.panel2.Controls.Add(this.label1);
            this.panel2.Dock = System.Windows.Forms.DockStyle.Top;
            this.panel2.Location = new System.Drawing.Point(0, 0);
            this.panel2.Name = "panel2";
            this.panel2.Size = new System.Drawing.Size(1240, 131);
            this.panel2.TabIndex = 0;
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label2.Location = new System.Drawing.Point(60, 19);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(115, 31);
            this.label2.TabIndex = 21;
            this.label2.Text = "Add Staff";
            // 
            // btnAdd
            // 
            this.btnAdd.AutoRoundedCorners = true;
            this.btnAdd.BorderRadius = 22;
            this.btnAdd.DisabledState.BorderColor = System.Drawing.Color.DarkGray;
            this.btnAdd.DisabledState.CustomBorderColor = System.Drawing.Color.DarkGray;
            this.btnAdd.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(169)))), ((int)(((byte)(169)))), ((int)(((byte)(169)))));
            this.btnAdd.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(141)))), ((int)(((byte)(141)))), ((int)(((byte)(141)))));
            this.btnAdd.Font = new System.Drawing.Font("Segoe UI", 10.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnAdd.ForeColor = System.Drawing.Color.White;
            this.btnAdd.Image = global::ResturantManagement.Properties.Resources.add__5_;
            this.btnAdd.ImageAlign = System.Windows.Forms.HorizontalAlignment.Right;
            this.btnAdd.ImageOffset = new System.Drawing.Point(10, 0);
            this.btnAdd.ImageSize = new System.Drawing.Size(25, 25);
            this.btnAdd.Location = new System.Drawing.Point(57, 67);
            this.btnAdd.Name = "btnAdd";
            this.btnAdd.Size = new System.Drawing.Size(116, 46);
            this.btnAdd.TabIndex = 25;
            this.btnAdd.Text = "Add";
            this.btnAdd.TextAlign = System.Windows.Forms.HorizontalAlignment.Left;
            this.btnAdd.TextOffset = new System.Drawing.Point(8, 0);
            this.btnAdd.Click += new System.EventHandler(this.btnAdd_Click);
            // 
            // txtSearch
            // 
            this.txtSearch.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.txtSearch.BorderRadius = 10;
            this.txtSearch.Cursor = System.Windows.Forms.Cursors.IBeam;
            this.txtSearch.DefaultText = "";
            this.txtSearch.DisabledState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(208)))), ((int)(((byte)(208)))), ((int)(((byte)(208)))));
            this.txtSearch.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(226)))), ((int)(((byte)(226)))), ((int)(((byte)(226)))));
            this.txtSearch.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtSearch.DisabledState.PlaceholderForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtSearch.FocusedState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtSearch.Font = new System.Drawing.Font("Segoe UI", 9F);
            this.txtSearch.HoverState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtSearch.IconLeft = ((System.Drawing.Image)(resources.GetObject("txtSearch.IconLeft")));
            this.txtSearch.Location = new System.Drawing.Point(920, 69);
            this.txtSearch.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.txtSearch.Name = "txtSearch";
            this.txtSearch.PasswordChar = '\0';
            this.txtSearch.PlaceholderText = "Search Here";
            this.txtSearch.SelectedText = "";
            this.txtSearch.Size = new System.Drawing.Size(281, 43);
            this.txtSearch.TabIndex = 20;
            this.txtSearch.TextChanged += new System.EventHandler(this.txtSearch_TextChanged);
            // 
            // label1
            // 
            this.label1.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(916, 23);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(90, 27);
            this.label1.TabIndex = 22;
            this.label1.Text = "Search";
            // 
            // panel1
            // 
            this.panel1.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.panel1.Controls.Add(this.lblUser);
            this.panel1.Controls.Add(this.lblStaff);
            this.panel1.Controls.Add(this.totalUser);
            this.panel1.Controls.Add(this.lblTotalStaff);
            this.panel1.Controls.Add(this.lblMgnUser);
            this.panel1.Controls.Add(this.lblMgnStaff);
            this.panel1.Dock = System.Windows.Forms.DockStyle.Bottom;
            this.panel1.Location = new System.Drawing.Point(0, 608);
            this.panel1.Name = "panel1";
            this.panel1.Size = new System.Drawing.Size(1248, 68);
            this.panel1.TabIndex = 16;
            // 
            // lblMgnStaff
            // 
            this.lblMgnStaff.Font = new System.Drawing.Font("Century Gothic", 12F, ((System.Drawing.FontStyle)((System.Drawing.FontStyle.Bold | System.Drawing.FontStyle.Underline))), System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblMgnStaff.ForeColor = System.Drawing.Color.White;
            this.lblMgnStaff.Image = ((System.Drawing.Image)(resources.GetObject("lblMgnStaff.Image")));
            this.lblMgnStaff.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.lblMgnStaff.Location = new System.Drawing.Point(7, 5);
            this.lblMgnStaff.Name = "lblMgnStaff";
            this.lblMgnStaff.Size = new System.Drawing.Size(114, 64);
            this.lblMgnStaff.TabIndex = 0;
            this.lblMgnStaff.Text = "Manage Staffs";
            this.lblMgnStaff.TextAlign = System.Drawing.ContentAlignment.MiddleRight;
            // 
            // metroTabControl1
            // 
            this.metroTabControl1.Controls.Add(this.staff);
            this.metroTabControl1.Controls.Add(this.user);
            this.metroTabControl1.Dock = System.Windows.Forms.DockStyle.Fill;
            this.metroTabControl1.Location = new System.Drawing.Point(0, 68);
            this.metroTabControl1.Name = "metroTabControl1";
            this.metroTabControl1.SelectedIndex = 0;
            this.metroTabControl1.Size = new System.Drawing.Size(1248, 608);
            this.metroTabControl1.TabIndex = 17;
            this.metroTabControl1.UseSelectable = true;
            this.metroTabControl1.SelectedIndexChanged += new System.EventHandler(this.metroTabControl1_SelectedIndexChanged);
            // 
            // user
            // 
            this.user.BackColor = System.Drawing.Color.White;
            this.user.Controls.Add(this.dgvUserInfo);
            this.user.Controls.Add(this.guna2Separator2);
            this.user.Controls.Add(this.panel3);
            this.user.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.user.Location = new System.Drawing.Point(4, 38);
            this.user.Name = "user";
            this.user.Size = new System.Drawing.Size(1240, 566);
            this.user.TabIndex = 1;
            this.user.Text = "User";
            // 
            // dgvUserInfo
            // 
            this.dgvUserInfo.AllowUserToAddRows = false;
            this.dgvUserInfo.AllowUserToDeleteRows = false;
            dataGridViewCellStyle4.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(238)))), ((int)(((byte)(239)))), ((int)(((byte)(249)))));
            this.dgvUserInfo.AlternatingRowsDefaultCellStyle = dataGridViewCellStyle4;
            this.dgvUserInfo.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            dataGridViewCellStyle5.Alignment = System.Windows.Forms.DataGridViewContentAlignment.MiddleLeft;
            dataGridViewCellStyle5.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(123)))), ((int)(((byte)(143)))), ((int)(((byte)(161)))));
            dataGridViewCellStyle5.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            dataGridViewCellStyle5.ForeColor = System.Drawing.Color.White;
            dataGridViewCellStyle5.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(123)))), ((int)(((byte)(143)))), ((int)(((byte)(161)))));
            dataGridViewCellStyle5.SelectionForeColor = System.Drawing.SystemColors.HighlightText;
            dataGridViewCellStyle5.WrapMode = System.Windows.Forms.DataGridViewTriState.True;
            this.dgvUserInfo.ColumnHeadersDefaultCellStyle = dataGridViewCellStyle5;
            this.dgvUserInfo.ColumnHeadersHeight = 40;
            this.dgvUserInfo.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.EnableResizing;
            this.dgvUserInfo.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.dgvsnoUser,
            this.dgvUserid,
            this.dgvuserame,
            this.dgvUserphone,
            this.dgvfullname,
            this.dgvUserpass,
            this.dgvuRole,
            this.editUser,
            this.deluser});
            dataGridViewCellStyle6.Alignment = System.Windows.Forms.DataGridViewContentAlignment.MiddleLeft;
            dataGridViewCellStyle6.BackColor = System.Drawing.Color.White;
            dataGridViewCellStyle6.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            dataGridViewCellStyle6.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            dataGridViewCellStyle6.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            dataGridViewCellStyle6.SelectionForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            dataGridViewCellStyle6.WrapMode = System.Windows.Forms.DataGridViewTriState.False;
            this.dgvUserInfo.DefaultCellStyle = dataGridViewCellStyle6;
            this.dgvUserInfo.GridColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.dgvUserInfo.Location = new System.Drawing.Point(53, 189);
            this.dgvUserInfo.Name = "dgvUserInfo";
            this.dgvUserInfo.ReadOnly = true;
            this.dgvUserInfo.RowHeadersVisible = false;
            this.dgvUserInfo.RowHeadersWidth = 51;
            this.dgvUserInfo.RowTemplate.DividerHeight = 1;
            this.dgvUserInfo.RowTemplate.Height = 35;
            this.dgvUserInfo.Size = new System.Drawing.Size(1155, 275);
            this.dgvUserInfo.TabIndex = 30;
            this.dgvUserInfo.ThemeStyle.AlternatingRowsStyle.BackColor = System.Drawing.Color.White;
            this.dgvUserInfo.ThemeStyle.AlternatingRowsStyle.Font = null;
            this.dgvUserInfo.ThemeStyle.AlternatingRowsStyle.ForeColor = System.Drawing.Color.Empty;
            this.dgvUserInfo.ThemeStyle.AlternatingRowsStyle.SelectionBackColor = System.Drawing.Color.Empty;
            this.dgvUserInfo.ThemeStyle.AlternatingRowsStyle.SelectionForeColor = System.Drawing.Color.Empty;
            this.dgvUserInfo.ThemeStyle.BackColor = System.Drawing.Color.White;
            this.dgvUserInfo.ThemeStyle.GridColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.dgvUserInfo.ThemeStyle.HeaderStyle.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(100)))), ((int)(((byte)(88)))), ((int)(((byte)(255)))));
            this.dgvUserInfo.ThemeStyle.HeaderStyle.BorderStyle = System.Windows.Forms.DataGridViewHeaderBorderStyle.None;
            this.dgvUserInfo.ThemeStyle.HeaderStyle.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.dgvUserInfo.ThemeStyle.HeaderStyle.ForeColor = System.Drawing.Color.White;
            this.dgvUserInfo.ThemeStyle.HeaderStyle.HeaightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.EnableResizing;
            this.dgvUserInfo.ThemeStyle.HeaderStyle.Height = 40;
            this.dgvUserInfo.ThemeStyle.ReadOnly = true;
            this.dgvUserInfo.ThemeStyle.RowsStyle.BackColor = System.Drawing.Color.White;
            this.dgvUserInfo.ThemeStyle.RowsStyle.BorderStyle = System.Windows.Forms.DataGridViewCellBorderStyle.SingleHorizontal;
            this.dgvUserInfo.ThemeStyle.RowsStyle.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.dgvUserInfo.ThemeStyle.RowsStyle.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            this.dgvUserInfo.ThemeStyle.RowsStyle.Height = 35;
            this.dgvUserInfo.ThemeStyle.RowsStyle.SelectionBackColor = System.Drawing.Color.FromArgb(((int)(((byte)(231)))), ((int)(((byte)(229)))), ((int)(((byte)(255)))));
            this.dgvUserInfo.ThemeStyle.RowsStyle.SelectionForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(71)))), ((int)(((byte)(69)))), ((int)(((byte)(94)))));
            this.dgvUserInfo.CellContentClick += new System.Windows.Forms.DataGridViewCellEventHandler(this.guna2DataGridView2_CellContentClick);
            // 
            // dgvsnoUser
            // 
            this.dgvsnoUser.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvsnoUser.HeaderText = "No";
            this.dgvsnoUser.MinimumWidth = 6;
            this.dgvsnoUser.Name = "dgvsnoUser";
            this.dgvsnoUser.ReadOnly = true;
            this.dgvsnoUser.Width = 71;
            // 
            // dgvUserid
            // 
            this.dgvUserid.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dgvUserid.HeaderText = "id";
            this.dgvUserid.MinimumWidth = 6;
            this.dgvUserid.Name = "dgvUserid";
            this.dgvUserid.ReadOnly = true;
            this.dgvUserid.Visible = false;
            this.dgvUserid.Width = 61;
            // 
            // dgvuserame
            // 
            this.dgvuserame.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill;
            this.dgvuserame.HeaderText = "User Name";
            this.dgvuserame.MinimumWidth = 6;
            this.dgvuserame.Name = "dgvuserame";
            this.dgvuserame.ReadOnly = true;
            // 
            // dgvUserphone
            // 
            this.dgvUserphone.HeaderText = "Phone";
            this.dgvUserphone.MinimumWidth = 6;
            this.dgvUserphone.Name = "dgvUserphone";
            this.dgvUserphone.ReadOnly = true;
            // 
            // dgvfullname
            // 
            this.dgvfullname.HeaderText = "Full Name";
            this.dgvfullname.MinimumWidth = 6;
            this.dgvfullname.Name = "dgvfullname";
            this.dgvfullname.ReadOnly = true;
            // 
            // dgvUserpass
            // 
            this.dgvUserpass.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill;
            this.dgvUserpass.HeaderText = "Password";
            this.dgvUserpass.MinimumWidth = 6;
            this.dgvUserpass.Name = "dgvUserpass";
            this.dgvUserpass.ReadOnly = true;
            // 
            // dgvuRole
            // 
            this.dgvuRole.HeaderText = "Role";
            this.dgvuRole.MinimumWidth = 6;
            this.dgvuRole.Name = "dgvuRole";
            this.dgvuRole.ReadOnly = true;
            // 
            // editUser
            // 
            this.editUser.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.editUser.HeaderText = "";
            this.editUser.Image = ((System.Drawing.Image)(resources.GetObject("editUser.Image")));
            this.editUser.MinimumWidth = 6;
            this.editUser.Name = "editUser";
            this.editUser.ReadOnly = true;
            this.editUser.Width = 6;
            // 
            // deluser
            // 
            this.deluser.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.deluser.HeaderText = "";
            this.deluser.Image = ((System.Drawing.Image)(resources.GetObject("deluser.Image")));
            this.deluser.MinimumWidth = 6;
            this.deluser.Name = "deluser";
            this.deluser.ReadOnly = true;
            this.deluser.Width = 6;
            // 
            // guna2Separator2
            // 
            this.guna2Separator2.Location = new System.Drawing.Point(53, 156);
            this.guna2Separator2.Name = "guna2Separator2";
            this.guna2Separator2.Size = new System.Drawing.Size(1187, 10);
            this.guna2Separator2.TabIndex = 29;
            // 
            // panel3
            // 
            this.panel3.Controls.Add(this.txtSearchUser);
            this.panel3.Controls.Add(this.label5);
            this.panel3.Controls.Add(this.btnAddUser);
            this.panel3.Controls.Add(this.label4);
            this.panel3.Dock = System.Windows.Forms.DockStyle.Top;
            this.panel3.Location = new System.Drawing.Point(0, 0);
            this.panel3.Name = "panel3";
            this.panel3.Size = new System.Drawing.Size(1240, 139);
            this.panel3.TabIndex = 1;
            // 
            // txtSearchUser
            // 
            this.txtSearchUser.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.txtSearchUser.BorderRadius = 10;
            this.txtSearchUser.Cursor = System.Windows.Forms.Cursors.IBeam;
            this.txtSearchUser.DefaultText = "";
            this.txtSearchUser.DisabledState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(208)))), ((int)(((byte)(208)))), ((int)(((byte)(208)))));
            this.txtSearchUser.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(226)))), ((int)(((byte)(226)))), ((int)(((byte)(226)))));
            this.txtSearchUser.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtSearchUser.DisabledState.PlaceholderForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtSearchUser.FocusedState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtSearchUser.Font = new System.Drawing.Font("Segoe UI", 9F);
            this.txtSearchUser.HoverState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtSearchUser.IconLeft = ((System.Drawing.Image)(resources.GetObject("txtSearchUser.IconLeft")));
            this.txtSearchUser.Location = new System.Drawing.Point(927, 79);
            this.txtSearchUser.Margin = new System.Windows.Forms.Padding(3, 4, 3, 4);
            this.txtSearchUser.Name = "txtSearchUser";
            this.txtSearchUser.PasswordChar = '\0';
            this.txtSearchUser.PlaceholderText = "Search Here";
            this.txtSearchUser.SelectedText = "";
            this.txtSearchUser.Size = new System.Drawing.Size(281, 43);
            this.txtSearchUser.TabIndex = 26;
            this.txtSearchUser.TextChanged += new System.EventHandler(this.txtSearchUser_TextChanged);
            // 
            // label5
            // 
            this.label5.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.label5.AutoSize = true;
            this.label5.Location = new System.Drawing.Point(927, 33);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(90, 27);
            this.label5.TabIndex = 28;
            this.label5.Text = "Search";
            // 
            // btnAddUser
            // 
            this.btnAddUser.AutoRoundedCorners = true;
            this.btnAddUser.BorderRadius = 22;
            this.btnAddUser.DisabledState.BorderColor = System.Drawing.Color.DarkGray;
            this.btnAddUser.DisabledState.CustomBorderColor = System.Drawing.Color.DarkGray;
            this.btnAddUser.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(169)))), ((int)(((byte)(169)))), ((int)(((byte)(169)))));
            this.btnAddUser.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(141)))), ((int)(((byte)(141)))), ((int)(((byte)(141)))));
            this.btnAddUser.Font = new System.Drawing.Font("Segoe UI", 10.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnAddUser.ForeColor = System.Drawing.Color.White;
            this.btnAddUser.Image = global::ResturantManagement.Properties.Resources.add__5_;
            this.btnAddUser.ImageAlign = System.Windows.Forms.HorizontalAlignment.Right;
            this.btnAddUser.ImageOffset = new System.Drawing.Point(10, 0);
            this.btnAddUser.ImageSize = new System.Drawing.Size(25, 25);
            this.btnAddUser.Location = new System.Drawing.Point(47, 79);
            this.btnAddUser.Name = "btnAddUser";
            this.btnAddUser.Size = new System.Drawing.Size(116, 46);
            this.btnAddUser.TabIndex = 31;
            this.btnAddUser.Text = "Add";
            this.btnAddUser.TextAlign = System.Windows.Forms.HorizontalAlignment.Left;
            this.btnAddUser.TextOffset = new System.Drawing.Point(8, 0);
            this.btnAddUser.Click += new System.EventHandler(this.btnAddUser_Click);
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label4.Location = new System.Drawing.Point(47, 31);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(112, 31);
            this.label4.TabIndex = 27;
            this.label4.Text = "Add User";
            // 
            // guna2Panel1
            // 
            this.guna2Panel1.Controls.Add(this.label3);
            this.guna2Panel1.Controls.Add(this.guna2PictureBox1);
            this.guna2Panel1.Dock = System.Windows.Forms.DockStyle.Top;
            this.guna2Panel1.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.guna2Panel1.Location = new System.Drawing.Point(0, 0);
            this.guna2Panel1.Name = "guna2Panel1";
            this.guna2Panel1.Size = new System.Drawing.Size(1248, 68);
            this.guna2Panel1.TabIndex = 15;
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.BackColor = System.Drawing.Color.Transparent;
            this.label3.Font = new System.Drawing.Font("Segoe UI", 14F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label3.ForeColor = System.Drawing.Color.White;
            this.label3.Location = new System.Drawing.Point(527, 19);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(295, 32);
            this.label3.TabIndex = 2;
            this.label3.Text = "Information User and Staff";
            // 
            // guna2PictureBox1
            // 
            this.guna2PictureBox1.BackColor = System.Drawing.Color.Transparent;
            this.guna2PictureBox1.Image = global::ResturantManagement.Properties.Resources.user_50px;
            this.guna2PictureBox1.ImageRotate = 0F;
            this.guna2PictureBox1.Location = new System.Drawing.Point(462, 7);
            this.guna2PictureBox1.Name = "guna2PictureBox1";
            this.guna2PictureBox1.Size = new System.Drawing.Size(59, 54);
            this.guna2PictureBox1.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom;
            this.guna2PictureBox1.TabIndex = 1;
            this.guna2PictureBox1.TabStop = false;
            this.guna2PictureBox1.UseTransparentBackground = true;
            // 
            // guna2MessageDialog1
            // 
            this.guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.OK;
            this.guna2MessageDialog1.Caption = "Staff List";
            this.guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
            this.guna2MessageDialog1.Parent = null;
            this.guna2MessageDialog1.Style = Guna.UI2.WinForms.MessageDialogStyle.Light;
            this.guna2MessageDialog1.Text = null;
            // 
            // guna2MessageDialog2
            // 
            this.guna2MessageDialog2.Buttons = Guna.UI2.WinForms.MessageDialogButtons.OK;
            this.guna2MessageDialog2.Caption = "User List";
            this.guna2MessageDialog2.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
            this.guna2MessageDialog2.Parent = null;
            this.guna2MessageDialog2.Style = Guna.UI2.WinForms.MessageDialogStyle.Light;
            this.guna2MessageDialog2.Text = null;
            // 
            // dataGridViewImageColumn1
            // 
            this.dataGridViewImageColumn1.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dataGridViewImageColumn1.HeaderText = "";
            this.dataGridViewImageColumn1.Image = ((System.Drawing.Image)(resources.GetObject("dataGridViewImageColumn1.Image")));
            this.dataGridViewImageColumn1.MinimumWidth = 6;
            this.dataGridViewImageColumn1.Name = "dataGridViewImageColumn1";
            this.dataGridViewImageColumn1.Width = 125;
            // 
            // dataGridViewImageColumn2
            // 
            this.dataGridViewImageColumn2.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.AllCells;
            this.dataGridViewImageColumn2.HeaderText = "";
            this.dataGridViewImageColumn2.Image = ((System.Drawing.Image)(resources.GetObject("dataGridViewImageColumn2.Image")));
            this.dataGridViewImageColumn2.MinimumWidth = 6;
            this.dataGridViewImageColumn2.Name = "dataGridViewImageColumn2";
            this.dataGridViewImageColumn2.Width = 125;
            // 
            // lblStaff
            // 
            this.lblStaff.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.lblStaff.AutoSize = true;
            this.lblStaff.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.lblStaff.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblStaff.ForeColor = System.Drawing.Color.White;
            this.lblStaff.Location = new System.Drawing.Point(1042, 20);
            this.lblStaff.Name = "lblStaff";
            this.lblStaff.Size = new System.Drawing.Size(141, 31);
            this.lblStaff.TabIndex = 7;
            this.lblStaff.Text = "Total Staffs  :";
            // 
            // lblTotalStaff
            // 
            this.lblTotalStaff.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.lblTotalStaff.AutoSize = true;
            this.lblTotalStaff.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.lblTotalStaff.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblTotalStaff.ForeColor = System.Drawing.Color.White;
            this.lblTotalStaff.Location = new System.Drawing.Point(1167, 20);
            this.lblTotalStaff.Name = "lblTotalStaff";
            this.lblTotalStaff.Size = new System.Drawing.Size(27, 31);
            this.lblTotalStaff.TabIndex = 8;
            this.lblTotalStaff.Text = "0";
            // 
            // totalUser
            // 
            this.totalUser.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.totalUser.AutoSize = true;
            this.totalUser.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.totalUser.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.totalUser.ForeColor = System.Drawing.Color.White;
            this.totalUser.Location = new System.Drawing.Point(1161, 20);
            this.totalUser.Name = "totalUser";
            this.totalUser.Size = new System.Drawing.Size(27, 31);
            this.totalUser.TabIndex = 8;
            this.totalUser.Text = "0";
            // 
            // lblUser
            // 
            this.lblUser.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.lblUser.AutoSize = true;
            this.lblUser.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(50)))), ((int)(((byte)(55)))), ((int)(((byte)(89)))));
            this.lblUser.Font = new System.Drawing.Font("Segoe UI", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblUser.ForeColor = System.Drawing.Color.White;
            this.lblUser.Location = new System.Drawing.Point(1042, 20);
            this.lblUser.Name = "lblUser";
            this.lblUser.Size = new System.Drawing.Size(132, 31);
            this.lblUser.TabIndex = 7;
            this.lblUser.Text = "Total User  :";
            // 
            // lblMgnUser
            // 
            this.lblMgnUser.Font = new System.Drawing.Font("Century Gothic", 12F, ((System.Drawing.FontStyle)((System.Drawing.FontStyle.Bold | System.Drawing.FontStyle.Underline))), System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblMgnUser.ForeColor = System.Drawing.Color.White;
            this.lblMgnUser.Image = ((System.Drawing.Image)(resources.GetObject("lblMgnUser.Image")));
            this.lblMgnUser.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.lblMgnUser.Location = new System.Drawing.Point(3, 4);
            this.lblMgnUser.Name = "lblMgnUser";
            this.lblMgnUser.Size = new System.Drawing.Size(114, 64);
            this.lblMgnUser.TabIndex = 0;
            this.lblMgnUser.Text = "Manage Users";
            this.lblMgnUser.TextAlign = System.Drawing.ContentAlignment.MiddleRight;
            // 
            // frmUserStaffInfo
            // 
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.None;
            this.ClientSize = new System.Drawing.Size(1248, 676);
            this.Controls.Add(this.panel1);
            this.Controls.Add(this.metroTabControl1);
            this.Controls.Add(this.guna2Panel1);
            this.Font = new System.Drawing.Font("Century Gothic", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Margin = new System.Windows.Forms.Padding(4);
            this.Name = "frmUserStaffInfo";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "frmUserStaffInfo";
            this.Load += new System.EventHandler(this.frmUserStaffInfo_Load);
            this.staff.ResumeLayout(false);
            ((System.ComponentModel.ISupportInitialize)(this.guna2DataGridView1)).EndInit();
            this.panel2.ResumeLayout(false);
            this.panel2.PerformLayout();
            this.panel1.ResumeLayout(false);
            this.panel1.PerformLayout();
            this.metroTabControl1.ResumeLayout(false);
            this.user.ResumeLayout(false);
            ((System.ComponentModel.ISupportInitialize)(this.dgvUserInfo)).EndInit();
            this.panel3.ResumeLayout(false);
            this.panel3.PerformLayout();
            this.guna2Panel1.ResumeLayout(false);
            this.guna2Panel1.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)(this.guna2PictureBox1)).EndInit();
            this.ResumeLayout(false);

        }

        #endregion
        private System.Windows.Forms.TabPage staff;
        private System.Windows.Forms.Panel panel2;
        private System.Windows.Forms.Panel panel1;
        private System.Windows.Forms.Label lblMgnStaff;
        private MetroFramework.Controls.MetroTabControl metroTabControl1;
        private System.Windows.Forms.TabPage user;
        private System.Windows.Forms.Panel panel3;
        public Guna.UI2.WinForms.Guna2PictureBox guna2PictureBox1;
        private Guna.UI2.WinForms.Guna2Panel guna2Panel1;
        public System.Windows.Forms.Label label3;
        public Guna.UI2.WinForms.Guna2MessageDialog guna2MessageDialog1;
        private Guna.UI2.WinForms.Guna2DataGridView guna2DataGridView1;
        private Guna.UI2.WinForms.Guna2Separator guna2Separator1;
        private System.Windows.Forms.Label label2;
        private Guna.UI2.WinForms.Guna2Button btnAdd;
        private Guna.UI2.WinForms.Guna2TextBox txtSearch;
        private System.Windows.Forms.Label label1;
        public Guna.UI2.WinForms.Guna2MessageDialog guna2MessageDialog2;
        private System.Windows.Forms.DataGridViewImageColumn dataGridViewImageColumn1;
        private System.Windows.Forms.DataGridViewImageColumn dataGridViewImageColumn2;
        private Guna.UI2.WinForms.Guna2TextBox txtSearchUser;
        private System.Windows.Forms.Label label5;
        private Guna.UI2.WinForms.Guna2DataGridView dgvUserInfo;
        private Guna.UI2.WinForms.Guna2Separator guna2Separator2;
        private Guna.UI2.WinForms.Guna2Button btnAddUser;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvSno;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvid;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvName;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvPhone;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvRole;
        private System.Windows.Forms.DataGridViewImageColumn dgvedit;
        private System.Windows.Forms.DataGridViewImageColumn dgvdel;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvsnoUser;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvUserid;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvuserame;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvUserphone;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvfullname;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvUserpass;
        private System.Windows.Forms.DataGridViewTextBoxColumn dgvuRole;
        private System.Windows.Forms.DataGridViewImageColumn editUser;
        private System.Windows.Forms.DataGridViewImageColumn deluser;
        private System.Windows.Forms.Label lblStaff;
        private System.Windows.Forms.Label lblTotalStaff;
        private System.Windows.Forms.Label lblUser;
        private System.Windows.Forms.Label totalUser;
        private System.Windows.Forms.Label lblMgnUser;
    }
}