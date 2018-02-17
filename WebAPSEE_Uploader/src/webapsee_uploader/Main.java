package webapsee_uploader;

import dropbox.DropboxUploader;
import gdrive.GoogleDriveUploader;
import java.util.Scanner;

public class Main {
    private static String op;
    private static IClient client;
    public static void main(String[] args) {    
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.println("\n ______[MENU]________\n"
                    + "|                    |\n"
                    + "| 1. Google Drive    |\n"
                    + "| 2. Dropbox         |\n"
                    + "| 0. Sair...         |\n"
                    + "|____________________|\n");
            System.out.print("# Escolha a opção: ");
            op = sc.nextLine();
            switch (op) {
                case "0":
                    System.out.println("Saindo...");
                    System.exit(0);
                    break;
                case "1":
                    client = new GoogleDriveUploader();
                    try {
                        client.execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case "2":
                    client = new DropboxUploader();
                    try {
                        client.execute();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                default:
                    System.err.println("\nOpção não listada!");
                    break;
            }            
        }
    }
}