package HyProTechTeam.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class AlloySmelterUiEvent {
    public static final BuilderCodec<AlloySmelterUiEvent> CODEC =
            BuilderCodec.<AlloySmelterUiEvent>builder(AlloySmelterUiEvent.class, AlloySmelterUiEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .addField(new KeyedCodec<>("DragItemStackId", new StringCodec()),
                            (event, value) -> event.dragItemStackId = value,
                            event -> event.dragItemStackId)
                    .addField(new KeyedCodec<>("DragItemStackQuantity", new IntegerCodec()),
                            (event, value) -> event.dragItemStackQuantity = value,
                            event -> event.dragItemStackQuantity)
                    .addField(new KeyedCodec<>("DragSourceInventorySectionId", new StringCodec()),
                            (event, value) -> event.dragSourceInventorySectionId = value,
                            event -> event.dragSourceInventorySectionId)
                    .addField(new KeyedCodec<>("DragSourceItemGridIndex", new IntegerCodec()),
                            (event, value) -> event.dragSourceItemGridIndex = value,
                            event -> event.dragSourceItemGridIndex)
                    .addField(new KeyedCodec<>("DragSourceSlotId", new IntegerCodec()),
                            (event, value) -> event.dragSourceSlotId = value,
                            event -> event.dragSourceSlotId)
                    .addField(new KeyedCodec<>("SlotIndex", new IntegerCodec()),
                            (event, value) -> event.slotIndex = value,
                            event -> event.slotIndex)
                    .addField(new KeyedCodec<>("SourceSlotId", new IntegerCodec()),
                            (event, value) -> event.sourceSlotId = value,
                            event -> event.sourceSlotId)
                    .addField(new KeyedCodec<>("SourceItemGridIndex", new IntegerCodec()),
                            (event, value) -> event.sourceItemGridIndex = value,
                            event -> event.sourceItemGridIndex)
                    .addField(new KeyedCodec<>("ItemStackId", new StringCodec()),
                            (event, value) -> event.itemStackId = value,
                            event -> event.itemStackId)
                    .addField(new KeyedCodec<>("ItemStackQuantity", new IntegerCodec()),
                            (event, value) -> event.itemStackQuantity = value,
                            event -> event.itemStackQuantity)
                    .addField(new KeyedCodec<>("PressedMouseButton", new IntegerCodec()),
                            (event, value) -> event.pressedMouseButton = value,
                            event -> event.pressedMouseButton)
                    .addField(new KeyedCodec<>("SourceInventorySectionId", new StringCodec()),
                            (event, value) -> event.sourceInventorySectionId = value,
                            event -> event.sourceInventorySectionId)
                    .addField(new KeyedCodec<>("Target", new StringCodec()),
                            (event, value) -> event.target = value,
                            event -> event.target)
                    .build();

    private String action;
    private String dragItemStackId;
    private Integer dragItemStackQuantity;
    private String dragSourceInventorySectionId;
    private Integer dragSourceItemGridIndex;
    private Integer dragSourceSlotId;
    private Integer slotIndex;
    private Integer sourceSlotId;
    private Integer sourceItemGridIndex;
    private String itemStackId;
    private Integer itemStackQuantity;
    private Integer pressedMouseButton;
    private String sourceInventorySectionId;
    private String target;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDragItemStackId() {
        return dragItemStackId;
    }

    public void setDragItemStackId(String dragItemStackId) {
        this.dragItemStackId = dragItemStackId;
    }

    public Integer getDragItemStackQuantity() {
        return dragItemStackQuantity;
    }

    public void setDragItemStackQuantity(Integer dragItemStackQuantity) {
        this.dragItemStackQuantity = dragItemStackQuantity;
    }

    public String getDragSourceInventorySectionId() {
        return dragSourceInventorySectionId;
    }

    public void setDragSourceInventorySectionId(String dragSourceInventorySectionId) {
        this.dragSourceInventorySectionId = dragSourceInventorySectionId;
    }

    public Integer getDragSourceItemGridIndex() {
        return dragSourceItemGridIndex;
    }

    public void setDragSourceItemGridIndex(Integer dragSourceItemGridIndex) {
        this.dragSourceItemGridIndex = dragSourceItemGridIndex;
    }

    public Integer getDragSourceSlotId() {
        return dragSourceSlotId;
    }

    public void setDragSourceSlotId(Integer dragSourceSlotId) {
        this.dragSourceSlotId = dragSourceSlotId;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Integer getSourceSlotId() {
        return sourceSlotId;
    }

    public void setSourceSlotId(Integer sourceSlotId) {
        this.sourceSlotId = sourceSlotId;
    }

    public Integer getSourceItemGridIndex() {
        return sourceItemGridIndex;
    }

    public void setSourceItemGridIndex(Integer sourceItemGridIndex) {
        this.sourceItemGridIndex = sourceItemGridIndex;
    }

    public String getItemStackId() {
        return itemStackId;
    }

    public void setItemStackId(String itemStackId) {
        this.itemStackId = itemStackId;
    }

    public Integer getItemStackQuantity() {
        return itemStackQuantity;
    }

    public void setItemStackQuantity(Integer itemStackQuantity) {
        this.itemStackQuantity = itemStackQuantity;
    }

    public Integer getPressedMouseButton() {
        return pressedMouseButton;
    }

    public void setPressedMouseButton(Integer pressedMouseButton) {
        this.pressedMouseButton = pressedMouseButton;
    }

    public String getSourceInventorySectionId() {
        return sourceInventorySectionId;
    }

    public void setSourceInventorySectionId(String sourceInventorySectionId) {
        this.sourceInventorySectionId = sourceInventorySectionId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
