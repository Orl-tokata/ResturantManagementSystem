//using Microsoft.Reporting.Map.WebForms.VirtualEarth;
using Microsoft.SqlServer.Server;
using ResturantManagement.View;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;

namespace ResturantManagement.Model
{
    public partial class frmProductAdd : Form
    {
        public frmProductAdd()
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
        // end alert message
        public int id = 0;
        public int cID = 0;
        private void frmProductAdd_Load(object sender, EventArgs e)
        {
            // for cb fill
            string qry = "select catID id , catName name from category ";

            ClassConnection.CBFill(qry,cbCat);

            if(cID > 0) // for update
            {
                cbCat.SelectedValue = cID;
            }

            if (id > 0)
            {
                ForUpdateLoadData();
            }
        }

        string filePath;
        Byte[] imageByteArray;

        private void btnBrowse_Click(object sender, EventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.Filter = "Images(.jpg, .png)|* .png; *.jpg";
            if(ofd.ShowDialog() == DialogResult.OK)
            {
                filePath = ofd.FileName;
                txtImage.Image = new Bitmap(filePath);
            }
        }
        private void btnSave_Click(object sender, EventArgs e)
        {
           string qry = "";
           if(txtName.Text != "" && txtPrice.Text != "" && cbCat.SelectedValue != null && txtImage.Image != null)
            {
                if (id == 0) // insert
                {
                    qry = "insert into products(pName,pPrice,CategoryID,pImage) values(@Name,@price, @cat ,@img)";
                }
                else // update
                {
                    qry = "update products set pName = @Name, pPrice = @price, CategoryID = @cat, pImage = @img where pID = @id ";
                }
                ClassConnection.con.Open();
                // for image
                Image temp = new Bitmap(txtImage.Image);
                MemoryStream ms = new MemoryStream();
                temp.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
                imageByteArray = ms.ToArray();

                SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
                cmd.Parameters.AddWithValue("@id", id);
                cmd.Parameters.Add("@Name", SqlDbType.NVarChar, 50);
                cmd.Parameters["@Name"].Value = txtName.Text;
                cmd.Parameters.AddWithValue("@price", double.Parse(txtPrice.Text));
                cmd.Parameters.AddWithValue("@cat", Convert.ToInt32(cbCat.SelectedValue));
                cmd.Parameters.AddWithValue("@img", imageByteArray);
                cmd.ExecuteNonQuery();
                //cmd.ExecuteScalar();
                //if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
                //if (id == 0) { id = Convert.ToInt32(cmd.ExecuteScalar()); } else { cmd.ExecuteNonQuery(); }
                //if (ClassConnection.con.State == ConnectionState.Open) { ClassConnection.con.Close(); }
                //if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
                ClassConnection.con.Close();
                if (id > 0)
                {
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    id = 0;
                    cID = 0;
                    txtName.Clear();
                    txtPrice.Clear();
                    cbCat.SelectedIndex = 0; 
                    cbCat.SelectedIndex = -1;
                    txtImage.Image = ResturantManagement.Properties.Resources.defaulPic;
                    txtName.Focus();
                    ClassConnection.con.Close();
                }
                else
                {
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    id = 0;
                    cID = 0;
                    txtName.Clear();
                    txtPrice.Clear();
                    cbCat.SelectedIndex = 0;
                    cbCat.SelectedIndex = -1;
                    txtImage.Image = ResturantManagement.Properties.Resources.defaulPic;
                    txtName.Focus();
                    ClassConnection.con.Close();
                }

            }
            else
            {
                //this.Alert("Please Provide Details!", AlertMessage.enmType.Warning);
                showToast("ERROR", "Please Provide Details!");
            }

        }

        private void ForUpdateLoadData()
        {
            string qry = @"Select * from products where pid = " + id + " ";
            SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            if(dt.Rows.Count > 0)
            {
                foreach(DataRow row in dt.Rows)
                {
                    Byte[] imageArray = (byte[])(row["pImage"]);
                    byte[] imageByteArray = imageArray;

                    txtName.Text = row["pName"].ToString();
                    txtPrice.Text = row["pPrice"].ToString(); 
                    txtImage.Image = Image.FromStream(new MemoryStream(imageByteArray));
                }
            }
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        // validation input number only
        private void txtPrice_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar) && e.KeyChar != '.' && e.KeyChar != ',';
        }
    }
}
