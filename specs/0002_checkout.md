Plan: Shopping Cart                                                                                       │
│                                                                                                           │
│ Context                                                                                                   │
│                                                                                                           │
│ The current ticketing flow supports selecting a single ticket type from a single event and going straight │
│  to payment. The goal is to allow customers to browse multiple events, add different tickets (different   │
│ types, different batches) to a cart, and check out everything in one go — creating one backend order per  │
│ event.                                                                                                    │
│                                                                                                           │
│ No backend changes are needed: the existing POST /v1/events/{eventId}/orders, POST                        │
│ /v1/orders/{orderId}/items, POST /v1/orders/{orderId}/checkout, and POST /v1/orders/{orderId}/purchase    │
│ endpoints already support this pattern. The cart is a pure frontend concern.                              │
│                                                                                                           │
│ The existing ticketing prototype components (ticket-selection-screen.tsx, payment-screen.tsx, etc.) are   │
│ hardcoded demos unrelated to the real API — they are left untouched. The cart is a new, parallel flow     │
│ that uses real API data.                                                                                  │
│                                                                                                           │
│ ---                                                                                                       │
│ Implementation Steps                                                                                      │
│                                                                                                           │
│ 1. Type: CartItem                                                                                         │
│                                                                                                           │
│ New file: web/src/types/cart.ts                                                                           │
│                                                                                                           │
│ export type CartItem = {                                                                                  │
│   eventId: string;                                                                                        │
│   eventName: string;                                                                                      │
│   ticketId: string;                                                                                       │
│   ticketName: string;                                                                                     │
│   batchId: string;                                                                                        │
│   priceInCents: number;                                                                                   │
│   quantity: number;       // always >= 1                                                                  │
│ };                                                                                                        │
│                                                                                                           │
│ 2. Cart Store                                                                                             │
│                                                                                                           │
│ New file: web/src/lib/cart-store.ts                                                                       │
│                                                                                                           │
│ Observer pattern — same structure as auth-store.ts and page-title-store.ts (pub/sub, no external lib).    │
│ Backed by localStorage so the cart survives page refresh.                                                 │
│                                                                                                           │
│ Exported API:                                                                                             │
│ - getCart(): CartItem[]                                                                                   │
│ - subscribeCart(fn): () => void                                                                           │
│ - addToCart(item: CartItem): void — if ticketId already exists, sums quantities (capped at maxPerOrder or │
│  sensible default)                                                                                        │
│ - setQuantity(ticketId: string, quantity: number): void — quantity = 0 removes the item                   │
│ - removeFromCart(ticketId: string): void                                                                  │
│ - clearCart(): void                                                                                       │
│ - getCartTotalItems(): number — sum of all quantities                                                     │
│ - getCartTotalCents(): number — sum of priceInCents * quantity                                            │
│                                                                                                           │
│ Internal: read/write localStorage key "beevent_cart" as JSON on every mutation.                           │
│                                                                                                           │
│ 3. Cart icon in Site Header                                                                               │
│                                                                                                           │
│ File to modify: web/src/components/layout/site-header.tsx                                                 │
│                                                                                                           │
│ - Import ShoppingCart from lucide-react                                                                   │
│ - Use useSyncExternalStore(subscribeCart, getCartTotalItems, getCartTotalItems) to get live count         │
│ - Render a Link to /cart with the ShoppingCart icon and a badge (count) when count > 0                    │
│ - Position: alongside the existing header buttons (before "Autoatendimento")                              │
│                                                                                                           │
│ 4. Add-to-Cart UI on Event Detail                                                                         │
│                                                                                                           │
│ File to modify: web/src/components/feature/catalog/event-detail-screen.tsx                                │
│                                                                                                           │
│ For each ticket row in the "Ingressos por lote" section, add:                                             │
│ - Inline quantity selector (– / + buttons, min 1, max ticket.max_per_order)                               │
│ - "Adicionar ao carrinho" button that calls addToCart({ eventId, eventName: event.name, ticketId:         │
│ ticket.id, ticketName: ticket.name, batchId: ticket.batch_id, priceInCents: ticket.price_cents, quantity  │
│ })                                                                                                        │
│ - If ticket is already in cart, show current cart quantity pre-filled                                     │
│ - Visual feedback: brief "Adicionado!" text after adding (local useState)                                 │
│                                                                                                           │
│ Since event-detail-screen.tsx is currently a Server Component (no "use client"), it must be made a client │
│  component or the add-to-cart logic extracted into a small "use client" child component. Preferred:       │
│ create a small client wrapper TicketCartButton component to keep the parent as lean as possible.          │
│                                                                                                           │
│ New file: web/src/components/feature/catalog/ticket-cart-button.tsx                                       │
│ - "use client" component                                                                                  │
│ - Props: ticket: CatalogTicket, eventId: string, eventName: string                                        │
│ - Internal state: quantity (default 1 or current cart quantity), added (boolean for feedback)             │
│ - Renders: quantity selector + "Adicionar" button                                                         │
│                                                                                                           │
│ 5. Cart Page                                                                                              │
│                                                                                                           │
│ New file: web/src/app/cart/page.tsx                                                                       │
│                                                                                                           │
│ Thin page wrapper exporting metadata + <Suspense> around <CartScreen />.                                  │
│                                                                                                           │
│ New file: web/src/components/feature/cart/cart-screen.tsx                                                 │
│ - "use client" component                                                                                  │
│ - Uses useSyncExternalStore(subscribeCart, getCart, getCart)                                              │
│ - Groups items by eventId for display                                                                     │
│ - Per item: event name, ticket name, price × qty, quantity –/+ (calls setQuantity), remove button (calls  │
│ removeFromCart)                                                                                           │
│ - Footer: total items count, total price, "Finalizar compra" button                                       │
│ - Empty state: message + "Ver eventos" link to /                                                          │
│ - Checkout button behavior:                                                                               │
│   - If not authenticated (useAuth().user === null): navigate to /login?next=/cart                         │
│   - If authenticated: call checkoutCart() from cart-checkout service (step 6)                             │
│                                                                                                           │
│ 6. Checkout Service                                                                                       │
│                                                                                                           │
│ New file: web/src/services/cart-checkout.ts                                                               │
│                                                                                                           │
│ export type CheckoutResult =                                                                              │
│   | { ok: true; orderIds: string[] }                                                                      │
│   | { ok: false; error: string };                                                                         │
│                                                                                                           │
│ export async function checkoutCart(items: CartItem[]): Promise<CheckoutResult>                            │
│                                                                                                           │
│ Algorithm:                                                                                                │
│ 1. Group items by eventId                                                                                 │
│ 2. For each event group (sequentially to avoid races):                                                    │
│ a. POST /v1/events/{eventId}/orders via authorizedFetch → get orderId                                     │
│ b. For each item in group: POST /v1/orders/{orderId}/items { ticket_id, quantity }                        │
│ c. POST /v1/orders/{orderId}/checkout                                                                     │
│ d. POST /v1/orders/{orderId}/purchase { provider: "manual" }                                              │
│ 3. On any error: return { ok: false, error } (do NOT clear cart, let user retry)                          │
│ 4. On full success: return { ok: true, orderIds } — caller clears cart and navigates                      │
│                                                                                                           │
│ cart-screen.tsx on success: calls clearCart(), navigates to /cart/confirmation?orders=id1,id2,...         │
│                                                                                                           │
│ 7. Confirmation Page                                                                                      │
│                                                                                                           │
│ New file: web/src/app/cart/confirmation/page.tsx                                                          │
│ New file: web/src/components/feature/cart/cart-confirmation-screen.tsx                                    │
│                                                                                                           │
│ - "use client" component                                                                                  │
│ - Reads ?orders=id1,id2 from useSearchParams()                                                            │
│ - Shows success state: list of order IDs, total (if available from store before clear, or omit)           │
│ - "Ver meus pedidos" → /account                                                                           │
│ - "Continuar comprando" → /                                                                               │
│                                                                                                           │
│ ---                                                                                                       │
│ Critical Files                                                                                            │
│                                                                                                           │
│ ┌──────────────────────────────────────────────────────────────┬────────────────────────────────────────┐ │
│ │                             File                             │                 Action                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/types/cart.ts                                        │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/lib/cart-store.ts                                    │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/components/feature/catalog/ticket-cart-button.tsx    │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/components/feature/cart/cart-screen.tsx              │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/components/feature/cart/cart-confirmation-screen.tsx │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/app/cart/page.tsx                                    │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/app/cart/confirmation/page.tsx                       │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/services/cart-checkout.ts                            │ Create                                 │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/components/layout/site-header.tsx                    │ Modify — add cart icon + badge         │ │
│ ├──────────────────────────────────────────────────────────────┼────────────────────────────────────────┤ │
│ │ web/src/components/feature/catalog/event-detail-screen.tsx   │ Modify — add TicketCartButton per      │ │
│ │                                                              │ ticket                                 │ │
│ └──────────────────────────────────────────────────────────────┴────────────────────────────────────────┘ │
│                                                                                                           │
│ Reuse                                                                                                     │
│                                                                                                           │
│ - authorizedFetch — web/src/services/api.ts (auth + auto-refresh)                                         │
│ - useAuth() — web/src/hooks/use-auth.ts (check login state before checkout)                               │
│ - Observer pattern — same as web/src/lib/page-title-store.ts and auth-store.ts                            │
│ - formatCurrencyBRL — web/src/lib/public-catalog.ts                                                       │
│ - shadcn/ui: Button, Badge, Card, CardHeader, CardContent — already installed                             │
│                                                                                                           │
│ ---                                                                                                       │
│ Verification                                                                                              │
│                                                                                                           │
│ 1. npm run typecheck — no errors                                                                          │
│ 2. npm run lint — no warnings                                                                             │
│ 3. Add ticket from event A → cart icon shows "1" in header                                                │
│ 4. Add different ticket from event B → cart shows "2", /cart shows 2 items grouped by event               │
│ 5. Change quantity → total price updates live                                                             │
│ 6. Remove item → cart updates; if last item → empty state shown                                           │
│ 7. Refresh page → cart items persist (localStorage)                                                       │
│ 8. Click "Finalizar compra" while logged out → redirect to /login?next=/cart                              │
│ 9. Click "Finalizar compra" while logged in → creates orders via API → navigates to /cart/confirmation    │
│ 10. /cart/confirmation shows order IDs and links back to catalog 