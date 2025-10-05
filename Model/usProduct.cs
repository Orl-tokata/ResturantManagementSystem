using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ResturantManagement.Model
{
    public partial class usProduct : UserControl
    {
        public usProduct()
        {
            InitializeComponent();
        }
        public event EventHandler onSelect = null;
        public int id { get; set; }
        public string PPrice { get; set; }
        public string PCategory { get; set; }

        private void txtImage_Click(object sender, EventArgs e)
        {
            onSelect?.Invoke(this, e);
        }

        public string pPrice
        {
            get { return lblPrice.Text; } set { lblPrice.Text = value + "$"; }
        }
        public string PName
        {
            get { return lblName.Text; } set { lblName.Text = value; }
        }
        public Image PImage
        {
            get { return txtImage.Image; } set { txtImage.Image = value; }
        }
    }
}
