package com.shop.shopbackend.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.shop.shopbackend.model.Product;
import com.shop.shopbackend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;
    // Add product
    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // Get all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setName(updatedProduct.getName());
        product.setBuyPrice(updatedProduct.getBuyPrice());
        product.setSellPrice(updatedProduct.getSellPrice());
        product.setStockQuantity(updatedProduct.getStockQuantity());
        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return "Product deleted successfully";
    }

    @GetMapping("/most-profitable")
    public Product getMostProfitableProduct() {
        return productRepository.findAll()
                .stream()
                .max((p1, p2) -> Double.compare(
                        (p1.getSellPrice() - p1.getBuyPrice()),
                        (p2.getSellPrice() - p2.getBuyPrice())
                ))
                .orElseThrow(() -> new RuntimeException("No products found"));
    }

    @GetMapping("/least-stock")
    public Product getLeastStockProduct() {
        return productRepository.findAll()
                .stream()
                .min((p1, p2) -> Integer.compare(
                        p1.getStockQuantity(),
                        p2.getStockQuantity()
                ))
                .orElseThrow(() -> new RuntimeException("No products found"));
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @GetMapping("/suggest")
    public List<Product> suggestProducts(@RequestParam String keyword) {
        return productRepository.findAll()
                .stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @GetMapping("/low-profit")
    public List<Product> getLowProfitProducts() {
        return productRepository.findAll()
                .stream()
                .filter(p -> (p.getSellPrice() - p.getBuyPrice()) < 10)
                .toList();
    }

    @GetMapping("/business-advice")
    public List<String> getBusinessAdvice() {
        List<String> advice = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            double profit = p.getSellPrice() - p.getBuyPrice();
            double margin = Math.round(((profit / p.getBuyPrice()) * 100) * 100.0) / 100.0;
            if (margin < 10) {
                advice.add(p.getName() + ": Very low profit (" + margin + "%) → Increase price");
            }
            else if (margin >= 10 && margin < 15) {
                advice.add(p.getName() + ": Low profit (" + margin + "%) → Optimize pricing");
            }
            else if (margin >= 15 && margin <= 25) {
                advice.add(p.getName() + ": Good product (" + margin + "% profit)");
            }
            else {
                advice.add(p.getName() + ": Excellent product (" + margin + "%) → Increase sales/marketing");
            }
            // Combine with stock logic
            if (p.getStockQuantity() < 10) {
                advice.add(p.getName() + ": Low stock → Restock soon");
            }
        }
        return advice;
    }

    @GetMapping("/report")
    public ResponseEntity<byte[]> generateReport() throws Exception {
        List<Product> products = productRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        document.add(new Paragraph("SHOP ANALYTICS REPORT")
                .setBold()
                .setFontSize(18));
        document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()));
        document.add(new Paragraph("\n"));
        double totalProfit = 0;
        double dailyProfit = 0;
        double weeklyProfit = 0;
        double monthlyProfit = 0;
        java.time.LocalDate today = java.time.LocalDate.now();
        com.itextpdf.layout.element.Table table =
                new com.itextpdf.layout.element.Table(5);
        table.addCell("Product");
        table.addCell("Buy Price");
        table.addCell("Sell Price");
        table.addCell("Profit");
        table.addCell("Stock");
        for (Product p : products) {
            double profit = p.getSellPrice() - p.getBuyPrice();
            totalProfit += profit;
            if (p.getCreatedAt() != null) {
                java.time.LocalDate date = p.getCreatedAt().toLocalDate();
                if (date.equals(today)) {
                    dailyProfit += profit;
                }
                if (date.isAfter(today.minusDays(7))) {
                    weeklyProfit += profit;
                }
                if (date.getMonth() == today.getMonth()) {
                    monthlyProfit += profit;
                }
            }
            table.addCell(p.getName());
            table.addCell(String.valueOf(p.getBuyPrice()));
            table.addCell(String.valueOf(p.getSellPrice()));
            table.addCell(String.valueOf(profit));
            table.addCell(String.valueOf(p.getStockQuantity()));
        }
        document.add(table);
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Total Profit: " + totalProfit).setBold());
        document.add(new Paragraph("Daily Profit: " + dailyProfit));
        document.add(new Paragraph("Weekly Profit: " + weeklyProfit));
        document.add(new Paragraph("Monthly Profit: " + monthlyProfit));
        document.close();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=report.pdf")
                .body(out.toByteArray());
    }

    @GetMapping("/barcode/{code}")
    public Product getProductByBarcode(@PathVariable String code) {
        return productRepository.findByBarcode(code);
    }
}