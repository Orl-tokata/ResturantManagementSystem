namespace ResturantManagement.View
{
    partial class frmAlertMessage
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
            this.btnSuccess = new System.Windows.Forms.Button();
            this.timer1 = new System.Windows.Forms.Timer(this.components);
            this.btnError = new System.Windows.Forms.Button();
            this.btnWarning = new System.Windows.Forms.Button();
            this.btnInfo = new System.Windows.Forms.Button();
            this.SuspendLayout();
            // 
            // btnSuccess
            // 
            this.btnSuccess.BackColor = System.Drawing.Color.SeaGreen;
            this.btnSuccess.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnSuccess.ForeColor = System.Drawing.Color.White;
            this.btnSuccess.Location = new System.Drawing.Point(157, 56);
            this.btnSuccess.Margin = new System.Windows.Forms.Padding(4);
            this.btnSuccess.Name = "btnSuccess";
            this.btnSuccess.Size = new System.Drawing.Size(210, 59);
            this.btnSuccess.TabIndex = 1;
            this.btnSuccess.Text = "Success";
            this.btnSuccess.UseVisualStyleBackColor = false;
            this.btnSuccess.Click += new System.EventHandler(this.btnSuccess_Click);
            // 
            // btnError
            // 
            this.btnError.BackColor = System.Drawing.Color.DarkRed;
            this.btnError.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnError.ForeColor = System.Drawing.Color.White;
            this.btnError.Location = new System.Drawing.Point(157, 123);
            this.btnError.Margin = new System.Windows.Forms.Padding(4);
            this.btnError.Name = "btnError";
            this.btnError.Size = new System.Drawing.Size(210, 59);
            this.btnError.TabIndex = 1;
            this.btnError.Text = "Error";
            this.btnError.UseVisualStyleBackColor = false;
            this.btnError.Click += new System.EventHandler(this.btnError_Click);
            // 
            // btnWarning
            // 
            this.btnWarning.BackColor = System.Drawing.Color.OrangeRed;
            this.btnWarning.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnWarning.ForeColor = System.Drawing.Color.White;
            this.btnWarning.Location = new System.Drawing.Point(157, 190);
            this.btnWarning.Margin = new System.Windows.Forms.Padding(4);
            this.btnWarning.Name = "btnWarning";
            this.btnWarning.Size = new System.Drawing.Size(210, 59);
            this.btnWarning.TabIndex = 1;
            this.btnWarning.Text = "Warning";
            this.btnWarning.UseVisualStyleBackColor = false;
            this.btnWarning.Click += new System.EventHandler(this.btnWarning_Click);
            // 
            // btnInfo
            // 
            this.btnInfo.BackColor = System.Drawing.Color.RoyalBlue;
            this.btnInfo.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnInfo.ForeColor = System.Drawing.Color.White;
            this.btnInfo.Location = new System.Drawing.Point(157, 257);
            this.btnInfo.Margin = new System.Windows.Forms.Padding(4);
            this.btnInfo.Name = "btnInfo";
            this.btnInfo.Size = new System.Drawing.Size(210, 59);
            this.btnInfo.TabIndex = 1;
            this.btnInfo.Text = "Info";
            this.btnInfo.UseVisualStyleBackColor = false;
            this.btnInfo.Click += new System.EventHandler(this.btnInfo_Click);
            // 
            // frmAlertMessage
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(12F, 23F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(512, 434);
            this.Controls.Add(this.btnInfo);
            this.Controls.Add(this.btnWarning);
            this.Controls.Add(this.btnError);
            this.Controls.Add(this.btnSuccess);
            this.Font = new System.Drawing.Font("Century Gothic", 12F);
            this.Margin = new System.Windows.Forms.Padding(4);
            this.Name = "frmAlertMessage";
            this.Text = "frmAlertMessage";
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.Button btnSuccess;
        private System.Windows.Forms.Timer timer1;
        private System.Windows.Forms.Button btnError;
        private System.Windows.Forms.Button btnWarning;
        private System.Windows.Forms.Button btnInfo;
    }
}