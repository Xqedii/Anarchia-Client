package dev.xqedii.xqediiclient.client.gui;

import dev.xqedii.xqediiclient.client.sniper.SnipeItem;
import dev.xqedii.xqediiclient.client.sniper.SnipingManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class EditSnipeItemScreen extends Screen {
    private final Screen parent;
    private final int itemIndex;
    private final SnipeItem itemToEdit;

    private TextFieldWidget nameField;
    private TextFieldWidget maxCostField;
    private CheckboxWidget durabilityCheckbox;
    private boolean isFullDurability;

    public EditSnipeItemScreen(Screen parent, int itemIndex) {
        super(Text.literal("Edit Snipe Item"));
        this.parent = parent;
        this.itemIndex = itemIndex;
        this.itemToEdit = SnipingManager.getItems().get(itemIndex);
        this.isFullDurability = itemToEdit.fullDurability;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 60, 200, 20, Text.literal("Item Name"));
        nameField.setText(itemToEdit.name);

        maxCostField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 25, 200, 20, Text.literal("Max Cost"));
        maxCostField.setText(String.valueOf(itemToEdit.maxCost));
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
            try {
                String name = nameField.getText();
                long maxCost = Long.parseLong(maxCostField.getText());
                if (!name.isEmpty()) {
                    SnipeItem updatedItem = new SnipeItem(name, isFullDurability, maxCost);
                    SnipingManager.updateItem(itemIndex, updatedItem);
                    this.client.setScreen(parent);
                }
            } catch (NumberFormatException ignored) {}
        }).dimensions(centerX - 100, centerY + 40, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(centerX + 5, centerY + 40, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, "Name:", this.width / 2 - 100, this.height / 2 - 72, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Max Cost:", this.width / 2 - 100, this.height / 2 - 37, 0xFFFFFF);
    }
}