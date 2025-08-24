package dev.xqedii.xqediiclient.client.gui;

import dev.xqedii.xqediiclient.client.sniper.SnipeItem;
import dev.xqedii.xqediiclient.client.sniper.SnipingManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AddSnipeItemScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget maxCostField;
    private CheckboxWidget durabilityCheckbox;
    private boolean isFullDurability = true;
    private boolean isDuplicate = false;

    public AddSnipeItemScreen(Screen parent) {
        super(Text.literal("Add Snipe Item"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 60, 200, 20, Text.literal("Item Name"));
        nameField.setChangedListener(this::onNameChange); // Listener do sprawdzania duplikatów

        maxCostField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 25, 200, 20, Text.literal("Max Cost"));
        maxCostField.setTextPredicate(s -> s.matches("[0-9]*"));

        durabilityCheckbox = CheckboxWidget.builder(Text.literal("Full Durability"), this.textRenderer)
                .pos(centerX - 100, centerY + 10)
                .checked(isFullDurability)
                .callback((box, checked) -> isFullDurability = checked)
                .build();

        this.addDrawableChild(nameField);
        this.addDrawableChild(maxCostField);
        this.addDrawableChild(durabilityCheckbox);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), button -> {
            if (isDuplicate) return; // Nie pozwalaj na dodanie duplikatu
            try {
                String name = nameField.getText();
                long maxCost = Long.parseLong(maxCostField.getText());
                if (!name.isEmpty()) {
                    SnipingManager.addItem(new SnipeItem(name, isFullDurability, maxCost));
                    this.client.setScreen(parent);
                }
            } catch (NumberFormatException ignored) {}
        }).dimensions(centerX - 100, centerY + 40, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(centerX + 5, centerY + 40, 95, 20).build());
    }

    private void onNameChange(String newName) {
        isDuplicate = SnipingManager.getItems().stream()
                .anyMatch(item -> item.name.equalsIgnoreCase(newName.trim()));
        nameField.setEditableColor(isDuplicate ? 0xFFE04B4B : 0xFFFFFFFF); // Czerwony kolor dla duplikatu
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, "Name:", this.width / 2 - 100, this.height / 2 - 72, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Max Cost:", this.width / 2 - 100, this.height / 2 - 37, 0xFFFFFF);
        if (isDuplicate) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("An item with this name already exists.").formatted(Formatting.RED),
                    this.width / 2 - textRenderer.getWidth("An item with this name already exists.") / 2, this.height / 2 + 65, 0);
        }
    }
}