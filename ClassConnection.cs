using System;
using System.Collections;
using System.Collections.Generic;
using System.Data.SqlClient;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;

namespace ResturantManagement
{
    internal class ClassConnection
    {
        public static string targetPath = @"Data Source=ResturantManagementSystem.sdf;Password=123";
        public static SqlCeConnection con = new SqlCeConnection(targetPath);

        // Method to check user validation 
        public static bool IsValidUser(string user, string pass)
        {
            bool isValid = false;
            string qry = @"select * from users where username ='" + user + "' and  upass = '" + pass + "'";
             
            SqlCeCommand cmd = new SqlCeCommand(qry, con);
            DataTable dt = new DataTable();
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            da.Fill(dt);
            if (dt.Rows.Count > 0)
            {
                foreach (DataRow row in dt.Rows)
                {
                    Byte[] imageArray = (byte[])(row["pImage"]);
                    byte[] imageByteArray = imageArray;

                    isValid = true;
                    USER = row["uName"].ToString();
                    Upass = row["upass"].ToString();
                    //Image = Image.FromStream(new MemoryStream(imageByteArray));
                }
            }
            return isValid;
        }

        public static void GetUser(string role)
        {
            string qry = @"select * from users where uRole = '" + role + "'";
            SqlCeCommand cmd = new SqlCeCommand(qry, con);
            DataTable dt = new DataTable();
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            da.Fill(dt);

            if (dt.Rows.Count > 0)
            {
                foreach (DataRow row in dt.Rows)
                {
                    Byte[] imageArray = (byte[])(row["pImage"]);
                    byte[] imageByteArray = imageArray;

                    Role = row["uRole"].ToString();
                    //Upass = row["upass"].ToString();
                    //Image = Image.FromStream(new MemoryStream(imageByteArray));
                }
            }
        }

        //create property username
        public static string user;
        public static string pass;
        public static string role;
        public static string USER
        {
            get { return user; }
            private set { user = value; }
        }
        public static string Upass
        {
            get { return pass; }
            private set { pass = value; }
        }
        public static string Role
        {
            get { return role; }
            private set { role = value; }
        }
        // Method for curd operation
        public static int SQl(string qry, Hashtable ht)
        {
            int res = 0;
            try
            {
                SqlCeCommand cmd = new SqlCeCommand(qry, con);
                cmd.CommandType = CommandType.Text;

                foreach (DictionaryEntry item in ht)
                {
                    cmd.Parameters.AddWithValue(item.Key.ToString(), item.Value);

                }
                if (con.State == ConnectionState.Closed) { con.Open(); }
                res = cmd.ExecuteNonQuery();
                if (con.State == ConnectionState.Open) { con.Close(); }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
                con.Close();
            }
            return res;
        }

        // for Loading data from Database
        public static void LoadData(string qry, DataGridView gv, ListBox lb)
        {
            // Serial no in Datagridview
            gv.CellFormatting += new DataGridViewCellFormattingEventHandler(gv_CellFormatting);
            try
            {
                SqlCeCommand cmd = new SqlCeCommand(qry, con);
                cmd.CommandType = CommandType.Text;
                SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
                DataTable dt = new DataTable();
                da.Fill(dt);
                for (int i = 0; i < lb.Items.Count; i++)
                {
                    string culNam1 = ((DataGridViewColumn)lb.Items[i]).Name;
                    gv.Columns[culNam1].DataPropertyName = dt.Columns[i].ToString();
                }
                gv.DataSource = dt;
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        private static void gv_CellFormatting(object sender, DataGridViewCellFormattingEventArgs e)
        {
            Guna.UI2.WinForms.Guna2DataGridView gv = (Guna.UI2.WinForms.Guna2DataGridView)sender;
            int count = 0;
            foreach (DataGridViewRow row in gv.Rows)
            {
                count++;
                row.Cells[0].Value = count;
            }
        }

        // add blue effect
        public static void BlueBackground(Form Model)
        {
            Form Background = new Form();
            using (Model)
            {
                Background.StartPosition = FormStartPosition.Manual;
                Background.FormBorderStyle = FormBorderStyle.None;
                Background.Opacity = 0.5d;
                Background.BackColor = Color.Black;
                Background.Size = frmMain.Instance.Size;
                Background.Location = frmMain.Instance.Location;
                Background.ShowInTaskbar = false;
                Background.Show();
                Model.Owner = Background;
                Model.ShowDialog(Background);
                Background.Dispose();
            }
        }

        // for cb fill
        public static void CBFill(string qry, ComboBox cb)
        {
            SqlCeCommand cmd = new SqlCeCommand(qry, con);
            cmd.CommandType = CommandType.Text;
            SqlCeDataAdapter da = new SqlCeDataAdapter(cmd);
            DataTable dt = new DataTable();
            da.Fill(dt);

            cb.DisplayMember = "Name";
            //cb.DisplayMember = "phone";
            cb.ValueMember = "id";
            cb.DataSource = dt;
            cb.SelectedIndex = -1;
        }
    }
}
