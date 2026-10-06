import { useState } from "react";
import { eventApi } from "../api";
import { useToast } from "../context/ToastContext";

/** Register / unregister for an event and update it in the given list setter. */
export default function useRsvp(setEvents) {
  const toast = useToast();
  const [busyId, setBusyId] = useState(null);

  const toggle = async (event) => {
    setBusyId(event.id);
    try {
      const res = event.registeredByMe ? await eventApi.unregister(event.id) : await eventApi.register(event.id);
      setEvents((list) =>
        list.map((e) => (e.id === event.id ? { ...e, registeredCount: res.registeredCount, registeredByMe: res.registeredByMe } : e)),
      );
      toast(res.registeredByMe ? `You're registered for ${event.title}.` : "Your seat has been released.");
    } catch (err) {
      toast(err.message, "error");
    } finally {
      setBusyId(null);
    }
  };

  return { toggle, busyId };
}
