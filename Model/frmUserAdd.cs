using ResturantManagement.View;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml.Linq;
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmUserAdd : Form
    {
        public frmUserAdd()
        {
            InitializeComponent();
        }
        // this Enable double buffering for all the controls
        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams handleParam = base.CreateParams;
                handleParam.ExStyle |= 0x02000000;
                return handleParam;
            }
        }
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        public int cID = 0;
        public int id = 0;

          private void frmUserAdd_Load(object sender, EventArgs e)
          {
                if (id > 0)
                {
                    forLoadUser();
                }
          }
        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }
        string filePath;
        Byte[] imageByteArray;
        private void btnSave_Click(object sender, EventArgs e)
        {
            string qry = "";
            if (txtUserName.Text == "" || txtFullName.Text == "" || txtPhone.Text == "" || txtPas.Text == "" || txtImageUser.Image == null || cbRole.SelectedItem == null)
            {
                //this.Alert("please input details!", AlertMessage.enmType.Warning);
                showToast("WARNING", "please input details!");
                return;
            }
            if (id == 0) // insert
            {
                qry = "insert into users (username,upass,uName,uphone,pImage,uRole,inTime,inStatus,inDate,OutTime,OutStatus,OutDate) values(@username,@upass,@fullname,@phone,@img,@uRole,@intime,@instatus,@indate,@outtime,@outstatus,@outdate)";
            }
            else // update
            {
                qry = "update users set username = @username, upass=@upass, uName = @fullname, uphone = @phone, pImage = @img, inTime=@intime, inStatus=@instatus,inDate=@indate,OutTime=@outtime,OutStatus=@outstatus,OutDate=@outdate,uRole=@uRole where userID = @id ";
            }

            // for image
            Image temp = new Bitmap(txtImageUser.Image);
            MemoryStream ms = new MemoryStream();
            temp.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
            imageByteArray = ms.ToArray();

            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            cmd.Parameters.AddWithValue("@id", id);
            cmd.Parameters.AddWithValue("@upass", txtPas.Text);
            cmd.Parameters.Add("@username", SqlDbType.NVarChar, 50);
            cmd.Parameters["@username"].Value = txtUserName.Text;
            cmd.Parameters.Add("@fullname", SqlDbType.NVarChar, 50);
            cmd.Parameters["@fullname"].Value = txtFullName.Text;
            cmd.Parameters.AddWithValue("@phone", txtPhone.Text);
            cmd.Parameters.AddWithValue("@img", imageByteArray);
            cmd.Parameters.AddWithValue("@intime", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@instatus", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@indate", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@outtime", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@outstatus", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@outdate", Convert.ToDouble(0));
            cmd.Parameters.AddWithValue("@uRole", cbRole.Text);

            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            if (id == 0) { id = Convert.ToInt32(cmd.ExecuteScalar()); } else { cmd.ExecuteNonQuery(); }
            if (ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }
            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            ClassConnection.con.Close();
            if (id > 0)
            {
                //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Saved Successfully.");
                id = 0;
                txtUserName.Clear();
                txtPhone.Clear();
                txtFullName.Clear();
                txtPas.Clear();
                cbRole.SelectedIndex = -1;
                txtImageUser.Image = ResturantManagement.Properties.Resources.defaulPic;
            }
            else
            {
                //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Saved Successfully.");
                id = 0;
                txtUserName.Clear();
                txtPhone.Clear();
                txtFullName.Clear();
                txtPas.Clear();
                cbRole.SelectedIndex = -1;
                txtImageUser.Image = ResturantManagement.Properties.Resources.defaulPic;
            }
        }

        private void btnBrowse_Click(object sender, EventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.Filter = "Images(.jpg, .png)|* .png; *.jpg";
            if (ofd.ShowDialog() == DialogResult.OK)
            {
                filePath = ofd.FileName;
                txtImageUser.Image = new Bitmap(filePath);
            }
        }
        private void forLoadUser()
        {
            string qry = @"Select * from users where userID = " + id + " ";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            if (dt.Rows.Count > 0)
            {
                foreach (DataRow row in dt.Rows)
                {
                    Byte[] imageArray = (byte[])(row["pImage"]);
                    byte[] imageByteArray = imageArray;

                    txtUserName.Text = row["username"].ToString();
                    txtFullName.Text = row["uName"].ToString();
                    txtPas.Text = row["upass"].ToString();
                    txtPhone.Text = row["uphone"].ToString();
                    txtImageUser.Image = Image.FromStream(new MemoryStream(imageByteArray));
                }
            }
        }

    }
}
