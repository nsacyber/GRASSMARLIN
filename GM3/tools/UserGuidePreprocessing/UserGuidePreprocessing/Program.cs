using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace UserGuidePreprocessing
{
    class Program
    {
        private static void BuildToC(System.Xml.XmlDocument document, System.Xml.XmlNode rootContents, System.Xml.XmlNode rootContainer, int level)
        {
            foreach(System.Xml.XmlNode child in rootContainer.SelectNodes(String.Format("h{0}[@id]", level)))
            {
                System.Xml.XmlNode xmlEntry = rootContents.AppendChild(document.CreateElement("li"));
                System.Xml.XmlNode xmlLink = xmlEntry.AppendChild(document.CreateElement("a"));
                xmlLink.Attributes.Append(document.CreateAttribute("href")).Value = String.Format("#{0}", child.Attributes["id"].Value);
                xmlLink.InnerText = child.InnerText;

                System.Xml.XmlNode xmlChildren = document.CreateElement("ul");
                BuildToC(document, xmlChildren, child.NextSibling, level + 1);
                if(xmlChildren.HasChildNodes)
                {
                    rootContents.AppendChild(xmlChildren);
                }
            }
        }
        static void Main(string[] args)
        {
            //Note: This app is all a bit of a hack since it is purpose-built for the existing build process, intended to have the output verified by a human tester.

            //We expect two arguments; the source and destination.
            if(args.Length != 2)
            {
                System.Console.WriteLine("Expected 2 parameters (Source File and Destination File).  {0} parameters were provided.", args.Length);
                return;
            }

            System.IO.FileInfo fiUserGuide = new System.IO.FileInfo(args[0]);
            if(!fiUserGuide.Exists)
            {
                System.Console.WriteLine("The given User Guide file ({0}) does not exist.", fiUserGuide.FullName);
                return;
            }

            //TODO: Consider changing this to a multi-pass stream processing routine.
            System.Xml.XmlDocument xmlUserGuide = new System.Xml.XmlDocument();
            xmlUserGuide.Load(fiUserGuide.FullName);

            //Step 1: Build the Table of Contents and embed it in the <UL> tag with an id of MainMenu.
            System.Xml.XmlNode xmlTableOfContentsRoot = xmlUserGuide.SelectSingleNode("//ul[@id='MainMenu']");
            if(xmlTableOfContentsRoot == null)
            {
                System.Console.WriteLine("Unable to locate the Table of Contents root (//ul[@id='MainMenu'])");
                return;
            }
            xmlTableOfContentsRoot.RemoveAll();
            //We only preserve the ID attribute.
            xmlTableOfContentsRoot.Attributes.Append(xmlUserGuide.CreateAttribute("id")).Value = "MainMenu";
            BuildToC(xmlUserGuide, xmlTableOfContentsRoot, xmlUserGuide.SelectSingleNode("//body"), 1);

            //Step 2: Insert code for collapsible sections
            foreach(System.Xml.XmlNode xmlCollapsibleBlock in xmlUserGuide.SelectNodes("//*[@class='collapsible']"))
            {
                System.Xml.XmlNode xmlSpan = xmlCollapsibleBlock.PrependChild(xmlUserGuide.CreateElement("span"));
                xmlSpan.Attributes.Append(xmlUserGuide.CreateAttribute("onClick")).Value = "this.parentNode.classList.toggle(\"collapsed\");void(0);";
                xmlSpan.Attributes.Append(xmlUserGuide.CreateAttribute("class")).Value = "control";
                System.Xml.XmlNode xmlImage = xmlSpan.AppendChild(xmlUserGuide.CreateElement("div"));
                xmlImage.Attributes.Append(xmlUserGuide.CreateAttribute("class")).Value = "image";
                xmlImage.InnerText = " ";
            }

            // Step 3: Replace all images with inlined data URIs
            foreach (System.Xml.XmlNode xmlImage in xmlTableOfContentsRoot.SelectNodes("//img[@src]"))
            {
                StringBuilder sbResult = new StringBuilder();
                try
                {
                    sbResult.Append("data:image/png;base64,");
                    System.IO.Stream streamContents = new System.IO.FileInfo(xmlImage.Attributes["src"].Value).OpenRead();
                    byte[] contents = new byte[streamContents.Length];
                    streamContents.Seek(0, System.IO.SeekOrigin.Begin);
                    streamContents.Read(contents, 0, contents.Length);
                    streamContents.Close();
                    sbResult.Append(System.Convert.ToBase64String(contents));
                }
                catch (Exception ex)
                {
                    sbResult.Clear();
                    sbResult.Append("about:blank");
                    xmlImage.Attributes.Append(xmlUserGuide.CreateAttribute("error")).Value = ex.Message;
                }
                xmlImage.Attributes["src"].Value = sbResult.ToString();
            }
            //TODO: Step 4: Build table of images, adjust figure names, if needed.

            // Step 5: Write the new file.
            System.IO.FileInfo fiOut = new System.IO.FileInfo(args[1]);
            if(fiOut.Exists)
            {
                fiOut.Delete();
            }
            var settings = new System.Xml.XmlWriterSettings();
            settings.OmitXmlDeclaration = true;
            using (var writer = System.Xml.XmlWriter.Create(fiOut.FullName, settings))
            {
                xmlUserGuide.WriteTo(writer);
            }
        }
    }
}
