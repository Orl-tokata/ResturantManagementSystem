namespace ResturantManagement.FormLogin
{
    partial class frmLogin
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
            this.components = new System.ComponentModel.Container();
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(frmLogin));
            this.lblForgotPass = new System.Windows.Forms.Label();
            this.label3 = new System.Windows.Forms.Label();
            this.label1 = new System.Windows.Forms.Label();
            this.txtusername = new Guna.UI2.WinForms.Guna2TextBox();
            this.btnShowPass = new System.Windows.Forms.Button();
            this.btnHidePass = new System.Windows.Forms.Button();
            this.pictureBox1 = new System.Windows.Forms.PictureBox();
            this.pictureBox2 = new System.Windows.Forms.PictureBox();
            this.txtPass = new Guna.UI2.WinForms.Guna2TextBox();
            this.btnLogin = new Guna.UI2.WinForms.Guna2Button();
            this.btnClose = new Guna.UI2.WinForms.Guna2Button();
            this.btnTimeOut = new System.Windows.Forms.Button();
            this.btnTimeIn = new System.Windows.Forms.Button();
            this.guna2MessageDialog1 = new Guna.UI2.WinForms.Guna2MessageDialog();
            this.timer1 = new System.Windows.Forms.Timer(this.components);
            this.timer2 = new System.Windows.Forms.Timer(this.components);
            this.label4 = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBox1)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBox2)).BeginInit();
            this.SuspendLayout();
            // 
            // lblForgotPass
            // 
            this.lblForgotPass.AutoSize = true;
            this.lblForgotPass.Font = new System.Drawing.Font("Khmer OS Battambang", 10.2F, System.Drawing.FontStyle.Underline, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblForgotPass.ForeColor = System.Drawing.SystemColors.MenuHighlight;
            this.lblForgotPass.Location = new System.Drawing.Point(479, 353);
            this.lblForgotPass.Name = "lblForgotPass";
            this.lblForgotPass.Size = new System.Drawing.Size(141, 31);
            this.lblForgotPass.TabIndex = 2;
            this.lblForgotPass.Text = "Forgot Password";
            this.lblForgotPass.Click += new System.EventHandler(this.lblForgotPass_Click);
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Font = new System.Drawing.Font("Khmer OS Battambang", 10.2F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label3.ForeColor = System.Drawing.Color.White;
            this.label3.Location = new System.Drawing.Point(390, 576);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(114, 31);
            this.label3.TabIndex = 27;
            this.label3.Text = "ងាយស្រួលប្រើ";
            // 
            // label1
            // 
            this.label1.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.label1.AutoSize = true;
            this.label1.Font = new System.Drawing.Font("Khmer Moul", 22.2F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label1.ForeColor = System.Drawing.Color.White;
            this.label1.Location = new System.Drawing.Point(233, 51);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(447, 72);
            this.label1.TabIndex = 21;
            this.label1.Text = "កម្មវិធីគ្រប់គ្រងភោជនីយដ្ឋាន";
            // 
            // txtusername
            // 
            this.txtusername.BorderRadius = 5;
            this.txtusername.Cursor = System.Windows.Forms.Cursors.IBeam;
            this.txtusername.DefaultText = "";
            this.txtusername.DisabledState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(208)))), ((int)(((byte)(208)))), ((int)(((byte)(208)))));
            this.txtusername.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(226)))), ((int)(((byte)(226)))), ((int)(((byte)(226)))));
            this.txtusername.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtusername.DisabledState.PlaceholderForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtusername.FocusedState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtusername.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.txtusername.HoverState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtusername.IconLeft = ((System.Drawing.Image)(resources.GetObject("txtusername.IconLeft")));
            this.txtusername.IconLeftOffset = new System.Drawing.Point(10, 0);
            this.txtusername.IconLeftSize = new System.Drawing.Size(25, 25);
            this.txtusername.Location = new System.Drawing.Point(275, 239);
            this.txtusername.Margin = new System.Windows.Forms.Padding(4, 6, 4, 6);
            this.txtusername.Name = "txtusername";
            this.txtusername.PasswordChar = '\0';
            this.txtusername.PlaceholderText = "User Name";
            this.txtusername.SelectedText = "";
            this.txtusername.Size = new System.Drawing.Size(314, 46);
            this.txtusername.TabIndex = 0;
            // 
            // btnShowPass
            // 
            this.btnShowPass.BackColor = System.Drawing.Color.White;
            this.btnShowPass.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnShowPass.Font = new System.Drawing.Font("Century Gothic", 10.2F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnShowPass.ForeColor = System.Drawing.Color.White;
            this.btnShowPass.Image = global::ResturantManagement.Properties.Resources.eye1;
            this.btnShowPass.Location = new System.Drawing.Point(528, 304);
            this.btnShowPass.Name = "btnShowPass";
            this.btnShowPass.Size = new System.Drawing.Size(56, 37);
            this.btnShowPass.TabIndex = 23;
            this.btnShowPass.Tag = "";
            this.btnShowPass.UseVisualStyleBackColor = false;
            this.btnShowPass.Click += new System.EventHandler(this.btnShowPass_Click);
            // 
            // btnHidePass
            // 
            this.btnHidePass.BackColor = System.Drawing.Color.White;
            this.btnHidePass.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnHidePass.ForeColor = System.Drawing.Color.White;
            this.btnHidePass.Image = global::ResturantManagement.Properties.Resources.invisible;
            this.btnHidePass.Location = new System.Drawing.Point(528, 304);
            this.btnHidePass.Name = "btnHidePass";
            this.btnHidePass.Size = new System.Drawing.Size(56, 37);
            this.btnHidePass.TabIndex = 30;
            this.btnHidePass.Tag = "";
            this.btnHidePass.UseVisualStyleBackColor = false;
            this.btnHidePass.Click += new System.EventHandler(this.btnHidePass_Click);
            // 
            // pictureBox1
            // 
            this.pictureBox1.Image = global::ResturantManagement.Properties.Resources.speaker_filled_audio_tool;
            this.pictureBox1.Location = new System.Drawing.Point(332, 580);
            this.pictureBox1.Name = "pictureBox1";
            this.pictureBox1.Size = new System.Drawing.Size(38, 25);
            this.pictureBox1.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom;
            this.pictureBox1.TabIndex = 29;
            this.pictureBox1.TabStop = false;
            // 
            // pictureBox2
            // 
            this.pictureBox2.Image = ((System.Drawing.Image)(resources.GetObject("pictureBox2.Image")));
            this.pictureBox2.Location = new System.Drawing.Point(44, 51);
            this.pictureBox2.Name = "pictureBox2";
            this.pictureBox2.Size = new System.Drawing.Size(155, 138);
            this.pictureBox2.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom;
            this.pictureBox2.TabIndex = 28;
            this.pictureBox2.TabStop = false;
            // 
            // txtPass
            // 
            this.txtPass.BorderRadius = 5;
            this.txtPass.Cursor = System.Windows.Forms.Cursors.IBeam;
            this.txtPass.DefaultText = "";
            this.txtPass.DisabledState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(208)))), ((int)(((byte)(208)))), ((int)(((byte)(208)))));
            this.txtPass.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(226)))), ((int)(((byte)(226)))), ((int)(((byte)(226)))));
            this.txtPass.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtPass.DisabledState.PlaceholderForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(138)))), ((int)(((byte)(138)))), ((int)(((byte)(138)))));
            this.txtPass.FocusedState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtPass.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.txtPass.HoverState.BorderColor = System.Drawing.Color.FromArgb(((int)(((byte)(94)))), ((int)(((byte)(148)))), ((int)(((byte)(255)))));
            this.txtPass.IconLeft = ((System.Drawing.Image)(resources.GetObject("txtPass.IconLeft")));
            this.txtPass.IconLeftOffset = new System.Drawing.Point(10, 0);
            this.txtPass.IconLeftSize = new System.Drawing.Size(25, 25);
            this.txtPass.Location = new System.Drawing.Point(275, 300);
            this.txtPass.Margin = new System.Windows.Forms.Padding(4, 6, 4, 6);
            this.txtPass.Name = "txtPass";
            this.txtPass.PasswordChar = '*';
            this.txtPass.PlaceholderText = "Password";
            this.txtPass.SelectedText = "";
            this.txtPass.Size = new System.Drawing.Size(314, 46);
            this.txtPass.TabIndex = 1;
            this.txtPass.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.txtPass_KeyPress);
            // 
            // btnLogin
            // 
            this.btnLogin.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(36)))), ((int)(((byte)(89)))), ((int)(((byte)(83)))));
            this.btnLogin.BorderRadius = 5;
            this.btnLogin.DisabledState.BorderColor = System.Drawing.Color.DarkGray;
            this.btnLogin.DisabledState.CustomBorderColor = System.Drawing.Color.DarkGray;
            this.btnLogin.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(169)))), ((int)(((byte)(169)))), ((int)(((byte)(169)))));
            this.btnLogin.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(141)))), ((int)(((byte)(141)))), ((int)(((byte)(141)))));
            this.btnLogin.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(14)))), ((int)(((byte)(131)))), ((int)(((byte)(136)))));
            this.btnLogin.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnLogin.ForeColor = System.Drawing.Color.White;
            this.btnLogin.Location = new System.Drawing.Point(275, 442);
            this.btnLogin.Name = "btnLogin";
            this.btnLogin.Size = new System.Drawing.Size(141, 45);
            this.btnLogin.TabIndex = 2;
            this.btnLogin.Text = "Login";
            this.btnLogin.Click += new System.EventHandler(this.btnLogin_TextChanged);
            // 
            // btnClose
            // 
            this.btnClose.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(36)))), ((int)(((byte)(89)))), ((int)(((byte)(83)))));
            this.btnClose.BorderRadius = 5;
            this.btnClose.DisabledState.BorderColor = System.Drawing.Color.DarkGray;
            this.btnClose.DisabledState.CustomBorderColor = System.Drawing.Color.DarkGray;
            this.btnClose.DisabledState.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(169)))), ((int)(((byte)(169)))), ((int)(((byte)(169)))));
            this.btnClose.DisabledState.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(141)))), ((int)(((byte)(141)))), ((int)(((byte)(141)))));
            this.btnClose.FillColor = System.Drawing.Color.FromArgb(((int)(((byte)(192)))), ((int)(((byte)(0)))), ((int)(((byte)(0)))));
            this.btnClose.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnClose.ForeColor = System.Drawing.Color.White;
            this.btnClose.Location = new System.Drawing.Point(448, 442);
            this.btnClose.Name = "btnClose";
            this.btnClose.Size = new System.Drawing.Size(141, 45);
            this.btnClose.TabIndex = 3;
            this.btnClose.Text = "Close";
            this.btnClose.Click += new System.EventHandler(this.btnClose_TextChanged);
            // 
            // btnTimeOut
            // 
            this.btnTimeOut.Font = new System.Drawing.Font("Century Gothic", 10.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnTimeOut.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(192)))), ((int)(((byte)(0)))), ((int)(((byte)(0)))));
            this.btnTimeOut.Image = ((System.Drawing.Image)(resources.GetObject("btnTimeOut.Image")));
            this.btnTimeOut.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.btnTimeOut.Location = new System.Drawing.Point(448, 387);
            this.btnTimeOut.Name = "btnTimeOut";
            this.btnTimeOut.Padding = new System.Windows.Forms.Padding(5, 0, 0, 0);
            this.btnTimeOut.Size = new System.Drawing.Size(136, 42);
            this.btnTimeOut.TabIndex = 5;
            this.btnTimeOut.Text = "Time Out";
            this.btnTimeOut.TextAlign = System.Drawing.ContentAlignment.MiddleRight;
            this.btnTimeOut.UseVisualStyleBackColor = true;
            this.btnTimeOut.Click += new System.EventHandler(this.btnTimeOut_Click);
            // 
            // btnTimeIn
            // 
            this.btnTimeIn.Font = new System.Drawing.Font("Century Gothic", 10.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.btnTimeIn.ForeColor = System.Drawing.Color.FromArgb(((int)(((byte)(0)))), ((int)(((byte)(0)))), ((int)(((byte)(192)))));
            this.btnTimeIn.Image = ((System.Drawing.Image)(resources.GetObject("btnTimeIn.Image")));
            this.btnTimeIn.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.btnTimeIn.Location = new System.Drawing.Point(275, 387);
            this.btnTimeIn.Margin = new System.Windows.Forms.Padding(5, 3, 3, 3);
            this.btnTimeIn.Name = "btnTimeIn";
            this.btnTimeIn.Padding = new System.Windows.Forms.Padding(9, 0, 9, 0);
            this.btnTimeIn.RightToLeft = System.Windows.Forms.RightToLeft.No;
            this.btnTimeIn.Size = new System.Drawing.Size(141, 42);
            this.btnTimeIn.TabIndex = 4;
            this.btnTimeIn.Text = "Time In";
            this.btnTimeIn.TextAlign = System.Drawing.ContentAlignment.MiddleRight;
            this.btnTimeIn.UseVisualStyleBackColor = true;
            this.btnTimeIn.Click += new System.EventHandler(this.btnTimeIn_Click);
            // 
            // guna2MessageDialog1
            // 
            this.guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.OK;
            this.guna2MessageDialog1.Caption = "Close Application";
            this.guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
            this.guna2MessageDialog1.Parent = null;
            this.guna2MessageDialog1.Style = Guna.UI2.WinForms.MessageDialogStyle.Light;
            this.guna2MessageDialog1.Text = null;
            // 
            // timer1
            // 
            this.timer1.Enabled = true;
            this.timer1.Interval = 40;
            this.timer1.Tick += new System.EventHandler(this.timer1_Tick);
            // 
            // timer2
            // 
            this.timer2.Interval = 40;
            this.timer2.Tick += new System.EventHandler(this.timer2_Tick);
            // 
            // label4
            // 
            this.label4.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.label4.AutoSize = true;
            this.label4.Font = new System.Drawing.Font("Khmer Moul", 22.2F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label4.ForeColor = System.Drawing.Color.White;
            this.label4.Location = new System.Drawing.Point(364, 100);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(140, 72);
            this.label4.TabIndex = 21;
            this.label4.Text = "មាតុភូមិ";
            // 
            // label2
            // 
            this.label2.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.label2.AutoSize = true;
            this.label2.Font = new System.Drawing.Font("Century Gothic", 13.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label2.ForeColor = System.Drawing.Color.White;
            this.label2.Location = new System.Drawing.Point(270, 171);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(365, 27);
            this.label2.TabIndex = 20;
            this.label2.Text = "Resturant Management System";
            // 
            // frmLogin
            // 
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.None;
            this.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(36)))), ((int)(((byte)(89)))), ((int)(((byte)(83)))));
            this.ClientSize = new System.Drawing.Size(796, 650);
            this.Controls.Add(this.btnTimeOut);
            this.Controls.Add(this.btnTimeIn);
            this.Controls.Add(this.btnClose);
            this.Controls.Add(this.btnLogin);
            this.Controls.Add(this.txtusername);
            this.Controls.Add(this.btnShowPass);
            this.Controls.Add(this.btnHidePass);
            this.Controls.Add(this.pictureBox1);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.lblForgotPass);
            this.Controls.Add(this.label3);
            this.Controls.Add(this.label4);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.pictureBox2);
            this.Controls.Add(this.txtPass);
            this.Font = new System.Drawing.Font("Century Gothic", 12F);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Name = "frmLogin";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "frmLogin";
            ((System.ComponentModel.ISupportInitialize)(this.pictureBox1)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBox2)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private Guna.UI2.WinForms.Guna2TextBox txtusername;
        private System.Windows.Forms.Button btnShowPass;
        private System.Windows.Forms.Button btnHidePass;
        private System.Windows.Forms.PictureBox pictureBox1;
        private System.Windows.Forms.Label lblForgotPass;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.PictureBox pictureBox2;
        private Guna.UI2.WinForms.Guna2TextBox txtPass;
        private System.Windows.Forms.Label label1;
        public Guna.UI2.WinForms.Guna2Button btnLogin;
        public Guna.UI2.WinForms.Guna2Button btnClose;
        private System.Windows.Forms.Button btnTimeOut;
        private System.Windows.Forms.Button btnTimeIn;
        private Guna.UI2.WinForms.Guna2MessageDialog guna2MessageDialog1;
        private System.Windows.Forms.Timer timer1;
        private System.Windows.Forms.Timer timer2;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.Label label2;
    }
}