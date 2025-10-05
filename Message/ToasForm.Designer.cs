namespace ResturantManagement
{
    partial class ToasForm
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
            this.toasBorder = new System.Windows.Forms.Panel();
            this.lblType = new System.Windows.Forms.Label();
            this.lblMessage = new System.Windows.Forms.Label();
            this.toastTimer = new System.Windows.Forms.Timer(this.components);
            this.toastHide = new System.Windows.Forms.Timer(this.components);
            this.picIcon = new System.Windows.Forms.PictureBox();
            ((System.ComponentModel.ISupportInitialize)(this.picIcon)).BeginInit();
            this.SuspendLayout();
            // 
            // toasBorder
            // 
            this.toasBorder.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(57)))), ((int)(((byte)(155)))), ((int)(((byte)(53)))));
            this.toasBorder.Location = new System.Drawing.Point(-5, -9);
            this.toasBorder.Margin = new System.Windows.Forms.Padding(4, 5, 4, 5);
            this.toasBorder.Name = "toasBorder";
            this.toasBorder.Size = new System.Drawing.Size(13, 79);
            this.toasBorder.TabIndex = 0;
            // 
            // lblType
            // 
            this.lblType.AutoSize = true;
            this.lblType.Font = new System.Drawing.Font("Segoe UI", 9F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblType.Location = new System.Drawing.Point(61, 8);
            this.lblType.Margin = new System.Windows.Forms.Padding(4, 0, 4, 0);
            this.lblType.Name = "lblType";
            this.lblType.Size = new System.Drawing.Size(42, 20);
            this.lblType.TabIndex = 2;
            this.lblType.Text = "Type";
            // 
            // lblMessage
            // 
            this.lblMessage.AutoSize = true;
            this.lblMessage.Font = new System.Drawing.Font("Segoe UI", 7.8F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.lblMessage.Location = new System.Drawing.Point(63, 33);
            this.lblMessage.Margin = new System.Windows.Forms.Padding(4, 0, 4, 0);
            this.lblMessage.Name = "lblMessage";
            this.lblMessage.Size = new System.Drawing.Size(88, 17);
            this.lblMessage.TabIndex = 2;
            this.lblMessage.Text = "Text Message";
            // 
            // toastTimer
            // 
            this.toastTimer.Enabled = true;
            this.toastTimer.Interval = 10;
            this.toastTimer.Tick += new System.EventHandler(this.toastTimer_Tick);
            // 
            // toastHide
            // 
            this.toastHide.Interval = 10;
            this.toastHide.Tick += new System.EventHandler(this.toastHide_Tick);
            // 
            // picIcon
            // 
            this.picIcon.Image = global::ResturantManagement.Properties.Resources.mWarning1;
            this.picIcon.Location = new System.Drawing.Point(18, 14);
            this.picIcon.Margin = new System.Windows.Forms.Padding(4, 5, 4, 5);
            this.picIcon.Name = "picIcon";
            this.picIcon.Size = new System.Drawing.Size(30, 30);
            this.picIcon.SizeMode = System.Windows.Forms.PictureBoxSizeMode.StretchImage;
            this.picIcon.TabIndex = 1;
            this.picIcon.TabStop = false;
            // 
            // ToasForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(11F, 28F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(298, 59);
            this.Controls.Add(this.lblMessage);
            this.Controls.Add(this.lblType);
            this.Controls.Add(this.picIcon);
            this.Controls.Add(this.toasBorder);
            this.Font = new System.Drawing.Font("Segoe UI", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Margin = new System.Windows.Forms.Padding(4, 5, 4, 5);
            this.Name = "ToasForm";
            this.Text = "ToasForm";
            this.Load += new System.EventHandler(this.ToasForm_Load);
            ((System.ComponentModel.ISupportInitialize)(this.picIcon)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Panel toasBorder;
        private System.Windows.Forms.PictureBox picIcon;
        private System.Windows.Forms.Label lblType;
        private System.Windows.Forms.Label lblMessage;
        private System.Windows.Forms.Timer toastTimer;
        private System.Windows.Forms.Timer toastHide;
    }
}