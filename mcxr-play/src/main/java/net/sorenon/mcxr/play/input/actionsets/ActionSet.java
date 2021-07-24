package net.sorenon.mcxr.play.input.actionsets;

import net.minecraft.client.resource.language.I18n;
import net.sorenon.mcxr.play.input.actions.Action;
import net.sorenon.mcxr.play.openxr.OpenXRInstance;
import net.sorenon.mcxr.play.openxr.OpenXRSession;
import net.sorenon.mcxr.play.openxr.XrException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrActionSet;
import org.lwjgl.openxr.XrActionSetCreateInfo;
import org.lwjgl.system.MemoryStack;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackMallocPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

public abstract class ActionSet implements AutoCloseable {

    public final String name;
    private XrActionSet handle;

    public ActionSet(String name) {
        this.name = name;
    }

    public abstract List<Action> actions();

    public boolean shouldSync() {
        return true;
    }

    public abstract void getBindings(HashMap<String, List<Pair<Action, String>>> map);

    public void sync(OpenXRSession session) {
        for (var action : actions()) {
            action.sync(session);
        }
    }

    public final void createHandle(OpenXRInstance instance) throws XrException {
        try (MemoryStack ignored = stackPush()) {
            String localizedName = "mcxr.actionset." + name;
            if (I18n.hasTranslation(localizedName)) {
                localizedName = I18n.translate(localizedName);
            }

            XrActionSetCreateInfo actionSetCreateInfo = XrActionSetCreateInfo.mallocStack().set(XR10.XR_TYPE_ACTION_SET_CREATE_INFO,
                    NULL,
                    memUTF8(name),
                    memUTF8(I18n.translate(localizedName)),
                    0
            );
            PointerBuffer pp = stackMallocPointer(1);
            instance.checkSafe(XR10.xrCreateActionSet(instance.handle, actionSetCreateInfo, pp), "xrCreateActionSet");
            handle = new XrActionSet(pp.get(0), instance.handle);

            for (var action : actions()) {
                action.createHandle(handle, instance);
            }
        }
    }

    public final XrActionSet getHandle() {
        return handle;
    }

    public final void destroyHandles() {
        if (handle != null) {
            XR10.xrDestroyActionSet(handle);
        }
    }

    @Override
    public final void close() {
        destroyHandles();
        for (var action : actions()) {
            action.close();
        }
    }
}